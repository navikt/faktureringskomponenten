package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
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
) {

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

        (fakturaserie.faktura as MutableList<Faktura>).add(nyFaktura)
        fakturaserieRepository.save(fakturaserie)

        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(nyFaktura, fakturaserie)
        fakturaBestiltProducer.produserBestillingsmelding(fakturaBestiltDto)

        return fakturaserie
    }
}
