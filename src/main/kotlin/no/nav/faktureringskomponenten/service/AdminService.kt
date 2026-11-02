package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.avregning.AvregningsfakturaGenerator
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.mappers.FakturaBestiltDtoMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ulid.ULID
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Service
class AdminService(
    val fakturaService: FakturaService,
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaBestiltProducer: FakturaBestiltProducer,
    private val fakturaBestillingService: FakturaBestillingService,
    private val fakturaRepository: FakturaRepository,
) {

    /**
     * Setter ny bestillingsdato på en faktura som ikke er bestilt ennå. Fakturaen bestilles av
     * cronjobben når datoen er nådd. Raden låses, så en samtidig erstatning eller kansellering
     * ikke overskrives.
     */
    @Transactional
    fun endreBestillingsdato(fakturaReferanse: String, nyBestillingsDato: LocalDate): EndreBestillingsdatoResultat {
        val faktura = fakturaRepository.findByReferanseNrForUpdate(fakturaReferanse) ?: return FakturaAvvist.FinnesIkke
        if (faktura.status != FakturaStatus.OPPRETTET) return FakturaAvvist.UgyldigStatus(faktura.status)

        val forrigeBestillingsDato = settBestillingsdato(faktura, nyBestillingsDato)

        return EndreBestillingsdatoResultat.Endret(fakturaReferanse, forrigeBestillingsDato, nyBestillingsDato)
    }

    /**
     * Setter bestillingsdato til i dag og bestiller fakturaen til OEBS i samme transaksjon.
     */
    @Transactional
    fun bestillUmiddelbart(fakturaReferanse: String): BestillFakturaResultat {
        val faktura = fakturaRepository.findByReferanseNrForUpdate(fakturaReferanse) ?: return FakturaAvvist.FinnesIkke
        if (faktura.status != FakturaStatus.OPPRETTET) return FakturaAvvist.UgyldigStatus(faktura.status)

        val bestillingsDato = LocalDate.now()
        val forrigeBestillingsDato = settBestillingsdato(faktura, bestillingsDato)
        // bestillFaktura leser fakturaen på nytt fra databasen, så den nye datoen må skrives først
        fakturaRepository.flush()
        fakturaBestillingService.bestillFaktura(fakturaReferanse, bestillingsDato)

        return BestillFakturaResultat.Bestilt(fakturaReferanse, forrigeBestillingsDato, bestillingsDato)
    }

    private fun settBestillingsdato(faktura: Faktura, nyBestillingsDato: LocalDate): LocalDate {
        val forrigeBestillingsDato = faktura.datoBestilt
        faktura.datoBestilt = nyBestillingsDato
        log.info("Endret bestillingsdato på faktura ${faktura.referanseNr} fra $forrigeBestillingsDato til $nyBestillingsDato")
        return forrigeBestillingsDato
    }

    @Transactional
    fun endreFødselsnummer(fakturaserieReferanse: String, nyttFødselsnummer: String) {
        val fakturaserie = fakturaserieRepository.findByReferanse(fakturaserieReferanse)
            ?: throw RessursIkkeFunnetException(
                field = "referanse",
                message = "Fant ikke fakturaserie med referanse: $fakturaserieReferanse"
            )

        if (fakturaserie.fodselsnummer == nyttFødselsnummer) {
            log.info("Fødselsnummer er allerede satt til ønsket verdi på fakturaserie $fakturaserieReferanse")
            return
        }

        fakturaserie.fodselsnummer = nyttFødselsnummer
        fakturaserieRepository.save(fakturaserie)

        log.info("Endret fødselsnummer på fakturaserie $fakturaserieReferanse")
    }

    /**
     * Setter status på samtlige fakturaer i en fakturaserie.
     * Returnerer fakturaene som faktisk ble endret, altså de som ikke allerede hadde [nyStatus].
     * En fakturaserie uten fakturaer er ikke en feil; da returneres tom liste.
     *
     * @throws RessursIkkeFunnetException hvis fakturaserien ikke finnes.
     */
    @Transactional
    fun endreStatusPaAlleFakturaer(fakturaserieReferanse: String, nyStatus: FakturaStatus): List<Faktura> {
        val fakturaserie = fakturaserieRepository.findByReferanse(fakturaserieReferanse)
            ?: throw RessursIkkeFunnetException(
                field = "fakturaserieReferanse",
                message = "Fant ikke fakturaserie med referanse: $fakturaserieReferanse"
            )

        if (fakturaserie.faktura.isEmpty()) {
            log.info("Fakturaserie $fakturaserieReferanse har ingen fakturaer, ingen status å endre")
            return emptyList()
        }

        val fakturaerSomSkalEndres = fakturaserie.faktura.filter { it.status != nyStatus }

        if (fakturaerSomSkalEndres.isEmpty()) {
            log.info(
                "Alle ${fakturaserie.faktura.size} fakturaer i fakturaserie $fakturaserieReferanse " +
                    "har allerede status $nyStatus"
            )
            return emptyList()
        }

        fakturaerSomSkalEndres.forEach { it.status = nyStatus }
        fakturaserieRepository.save(fakturaserie)

        log.info(
            "Endret status til $nyStatus på ${fakturaerSomSkalEndres.size} av " +
                "${fakturaserie.faktura.size} fakturaer i fakturaserie $fakturaserieReferanse"
        )
        return fakturaerSomSkalEndres
    }

    @Transactional
    fun krediterFaktura(fakturaReferanse: String): Fakturaserie {
        val faktura = fakturaService.hentFaktura(fakturaReferanse) ?: throw RessursIkkeFunnetException(
            field = "fakturaReferanse",
            message = "Fant ikke faktura med referanse: $fakturaReferanse"
        )

        val fakturaserie = faktura.fakturaserie ?: throw RessursIkkeFunnetException(
            field = "fakturaserie",
            message = "Fant ikke fakturaserie for faktura med referanse: $fakturaReferanse"
        )

        if (faktura.status != FakturaStatus.BESTILT) {
            throw IllegalStateException("Faktura er ikke i BESTILT status")
        }

        faktura.erKreditnota = true

        val nyFaktura = Faktura(
            referanseNr = ULID.randomULID(),
            fakturaserie = fakturaserie,
            status = FakturaStatus.BESTILT,
            datoBestilt = LocalDate.now(),
            fakturaLinje = faktura.fakturaLinje.map {
                FakturaLinje(
                    enhetsprisPerManed = it.belop,
                    periodeFra = it.periodeFra,
                    periodeTil = it.periodeTil,
                    antall = BigDecimal(-1),
                    belop = it.belop.negate(),
                    beskrivelse = AvregningsfakturaGenerator.genererBeskrivelse(
                        it.periodeFra,
                        it.periodeTil,
                        it.belop.negate(),
                        it.belop
                    ),
                )
            },
            krediteringFakturaRef = faktura.referanseNr,
            referertFakturaVedAvregning = faktura,
            erKreditnota = true,
        )

        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(nyFaktura, fakturaserie)
        nyFaktura.beskrivelse = fakturaBestiltDto.beskrivelse

        (fakturaserie.faktura as MutableList<Faktura>).add(nyFaktura)
        fakturaserieRepository.save(fakturaserie)

        fakturaBestiltProducer.produserBestillingsmelding(fakturaBestiltDto)

        return fakturaserie
    }
}

sealed interface EndreBestillingsdatoResultat {
    data class Endret(
        val fakturaReferanse: String,
        val forrigeBestillingsDato: LocalDate,
        val nyBestillingsDato: LocalDate
    ) : EndreBestillingsdatoResultat
}

sealed interface BestillFakturaResultat {
    data class Bestilt(
        val fakturaReferanse: String,
        val forrigeBestillingsDato: LocalDate,
        val bestillingsDato: LocalDate
    ) : BestillFakturaResultat
}

/** Fakturaen kan ikke endres eller bestilles. Felles for [EndreBestillingsdatoResultat] og [BestillFakturaResultat]. */
sealed interface FakturaAvvist : EndreBestillingsdatoResultat, BestillFakturaResultat {
    data object FinnesIkke : FakturaAvvist

    data class UgyldigStatus(val status: FakturaStatus) : FakturaAvvist
}
