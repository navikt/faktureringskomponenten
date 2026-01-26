package no.nav.faktureringskomponenten.service

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.faktureringskomponenten.config.ToggleName
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ulid.ULID
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Service
class FakturaserieService(
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaserieGenerator: FakturaserieGenerator,
    private val unleash: Unleash
) {

    fun hentFakturaserie(referanse: String): Fakturaserie =
        fakturaserieRepository.findByReferanse(referanse) ?: throw RessursIkkeFunnetException(
            field = "referanse",
            message = "Fant ikke fakturaserie på: $referanse"
        )

    fun hentFakturaserier(referanse: String): List<Fakturaserie> {
        return fakturaserieRepository.findAllByReferanse(referanse)
    }

    @Transactional
    fun lagNyFakturaserie(fakturaserieDto: FakturaserieDto, forrigeReferanse: String? = null): String {
        if (!forrigeReferanse.isNullOrEmpty()) {
            return erstattFakturaserie(forrigeReferanse, fakturaserieDto)
        }

        val fakturaserie = fakturaserieGenerator.lagFakturaserie(fakturaserieDto)
        fakturaserieRepository.save(fakturaserie)
        log.info("Lagret fakturaserie: $fakturaserie")

        val listeAvAktiveFakturaserierForFodselsnummer =
            fakturaserieRepository.findAllByFodselsnummer(fakturaserie.fodselsnummer)
                .filter { it.erAktiv() }
        if (listeAvAktiveFakturaserierForFodselsnummer.size > 1) {
            log.warn("Det finnes flere aktive fakturaserier for fødselsnummer av fakturaserie ${fakturaserie.referanse}")
        }
        return fakturaserie.referanse
    }

    private fun erstattFakturaserie(opprinneligReferanse: String, fakturaserieDto: FakturaserieDto): String {
        val opprinneligFakturaserie = fakturaserieRepository.findByReferanse(opprinneligReferanse)
            ?: throw RessursIkkeFunnetException(
                field = "referanse",
                message = "Fant ikke opprinnelig fakturaserie med referanse $opprinneligReferanse"
            )
        check(opprinneligFakturaserie.erAktiv()) { "Bare aktiv fakturaserie kan erstattes" }

        if (unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER) && harIngenAvregning(
                fakturaserieDto,
                opprinneligFakturaserie
            )
        ) {
            log.info("Ingen avregning nødvendig ved erstatning av fakturaserie ${opprinneligFakturaserie.referanse}, returnerer opprinnelig referanse")
            opprinneligFakturaserie.avbrytPlanlagteFakturaer()
            fakturaserieRepository.save(opprinneligFakturaserie)
            return opprinneligFakturaserie.referanse
        }
        val nyFakturaserie = fakturaserieGenerator.lagFakturaserieForEndring(
            fakturaserieDto,
            opprinneligFakturaserie
        )
        fakturaserieRepository.save(nyFakturaserie)

        opprinneligFakturaserie.erstattMed(nyFakturaserie)
        fakturaserieRepository.save(opprinneligFakturaserie)

        log.info("Erstattet fakturaserie: ${opprinneligFakturaserie.referanse}, lagret ny: ${nyFakturaserie.referanse}")
        return nyFakturaserie.referanse
    }

    private fun harIngenAvregning(
        fakturaserieDto: FakturaserieDto,
        opprinneligFakturaserie: Fakturaserie
    ): Boolean {
        val bestilteFakturaFraIÅrOgFremover = opprinneligFakturaserie.bestilteFakturaer().filter { it.alleFakturaLinjerErFraIÅrEllerFremover() }
        return fakturaserieDto.perioder.isEmpty() && bestilteFakturaFraIÅrOgFremover.isEmpty()
    }

    fun finnesReferanse(referanse: String): Boolean {
        return fakturaserieRepository.findByReferanse(referanse) != null
    }

    @Transactional
    fun endreFakturaMottaker(fakturaserieReferanse: String, fakturamottakerDto: FakturamottakerDto) {
        val fakturaserie = hentFakturaserie(fakturaserieReferanse)
        val gjenståendeFakturaer = fakturaserie.planlagteFakturaer()

        if (!mottakerErEndret(fakturaserie, fakturamottakerDto) || gjenståendeFakturaer.isEmpty()) {
            log.info("Fakturamottaker ikke endret på fakturaserie: $fakturaserieReferanse")
            return
        }

        fakturaserie.fullmektig = fakturamottakerDto.fullmektig
        log.info("Fakturamottaker endret på fakturaserie: $fakturaserieReferanse")
        fakturaserieRepository.save(fakturaserie)
    }

    private fun mottakerErEndret(fakturaserie: Fakturaserie, fakturamottakerDto: FakturamottakerDto) =
        fakturamottakerDto.fullmektig != fakturaserie.fullmektig


    @Transactional
    fun lagNyFaktura(fakturaDto: FakturaDto): String {
        val krediteringFakturaRef = hentKrediteringFakturaRef(fakturaDto.tidligereFakturaserieReferanse)

        return Fakturaserie(
            id = null,
            referanse = fakturaDto.referanse,
            fakturaGjelderInnbetalingstype = fakturaDto.fakturaGjelderInnbetalingstype,
            fodselsnummer = fakturaDto.fodselsnummer,
            fullmektig = fakturaDto.fullmektig,
            referanseBruker = fakturaDto.referanseBruker,
            referanseNAV = fakturaDto.referanseNAV,
            startdato = fakturaDto.startDato,
            sluttdato = fakturaDto.sluttDato,
            intervall = FakturaserieIntervall.SINGEL,
            faktura = listOf(
                Faktura(
                    referanseNr = ULID.randomULID(),
                    datoBestilt = LocalDate.now(),
                    krediteringFakturaRef = krediteringFakturaRef ?: "",
                    fakturaLinje = listOf(
                        FakturaLinje(
                            periodeFra = fakturaDto.startDato,
                            periodeTil = fakturaDto.sluttDato,
                            belop = fakturaDto.belop,
                            antall = BigDecimal.ONE,
                            beskrivelse = fakturaDto.beskrivelse,
                            enhetsprisPerManed = fakturaDto.belop
                        )
                    )
                )
            )
        ).also {
            fakturaserieRepository.save(it)
            log.info("Lagret fakturaserie: $it")
        }.referanse
    }

    private fun hentKrediteringFakturaRef(
        tidligereFakturaserieReferanse: String?,
    ): String? {
        val tidligereFakturaserie =
            fakturaserieRepository.findByReferanse(tidligereFakturaserieReferanse) ?: return null

        require(
            tidligereFakturaserie.status !in setOf(
                FakturaserieStatus.ERSTATTET,
                FakturaserieStatus.KANSELLERT
            )
        ) { "Tidligere fakturaserie med ref ${tidligereFakturaserie.referanse} er i feil status: ${tidligereFakturaserie.status}" }

        return tidligereFakturaserie.faktura.single().krediteringFakturaRef
    }

}
