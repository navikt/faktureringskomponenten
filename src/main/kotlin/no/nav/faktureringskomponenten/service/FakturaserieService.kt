package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.avregning.AvregningBehandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ulid.ULID
import java.math.BigDecimal

private val log = KotlinLogging.logger { }

@Service
class FakturaserieService(
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaserieGenerator: FakturaserieGenerator,
    private val avregningBehandler: AvregningBehandler,
    private val fakturaBestillingService: FakturaBestillingService,
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
            log.error("Det finnes flere aktive fakturaserier for fødselsnummer av fakturaserie ${fakturaserie.referanse}")
        }
        return fakturaserie.referanse
    }

    fun erstattFakturaserie(opprinneligReferanse: String, fakturaserieDto: FakturaserieDto): String {
        val opprinneligFakturaserie = fakturaserieRepository.findByReferanse(opprinneligReferanse)
            ?: throw RessursIkkeFunnetException(
                field = "referanse",
                message = "Fant ikke opprinnelig fakturaserie med referanse $opprinneligReferanse"
            )

        check(opprinneligFakturaserie.erAktiv()) { "Bare aktiv fakturaserie kan erstattes" }

        val nyFakturaserie = fakturaserieGenerator.lagFakturaserie(
            fakturaserieDto,
            finnStartDatoForFørstePlanlagtFaktura(opprinneligFakturaserie),
            avregningBehandler.lagAvregningsfakturaer(
                fakturaserieDto.perioder,
                opprinneligFakturaserie.bestilteFakturaer()
            )
        )
        fakturaserieRepository.save(nyFakturaserie)

        opprinneligFakturaserie.erstattMed(nyFakturaserie)
        fakturaserieRepository.save(opprinneligFakturaserie)

        log.info("Kansellert fakturaserie: ${opprinneligFakturaserie.referanse}, lagret ny: ${nyFakturaserie.referanse}")
        return nyFakturaserie.referanse
    }

    private fun finnStartDatoForFørstePlanlagtFaktura(opprinneligFakturaserie: Fakturaserie) =
        if (opprinneligFakturaserie.erUnderBestilling()) {
            opprinneligFakturaserie.planlagteFakturaer().minByOrNull { it.getPeriodeFra() }?.getPeriodeFra()
        } else null

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
    fun kansellerFakturaserie(referanse: String): String {
        val eksisterendeFakturaserie = fakturaserieRepository.findByReferanse(referanse)
            ?: throw throw RessursIkkeFunnetException(
                field = "fakturaserieId",
                message = "Finner ikke fakturaserie med referanse $referanse"
            )

        val alleFakturaserier = hentFakturaserier(eksisterendeFakturaserie.referanse)
        val startDato = alleFakturaserier.minBy { it.startdato }.startdato
        val sluttDato = alleFakturaserier.maxBy { it.startdato }.sluttdato

        val fakturaserieDto = FakturaserieDto(
            fakturaserieReferanse = ULID.randomULID(),
            fodselsnummer = eksisterendeFakturaserie.fodselsnummer,
            fullmektig = eksisterendeFakturaserie.fullmektig,
            referanseBruker = eksisterendeFakturaserie.referanseBruker,
            referanseNAV = eksisterendeFakturaserie.referanseNAV,
            fakturaGjelderInnbetalingstype = eksisterendeFakturaserie.fakturaGjelderInnbetalingstype,
            intervall = eksisterendeFakturaserie.intervall,
            perioder = listOf(FakturaseriePeriode(BigDecimal.ZERO, startDato, sluttDato, "Kansellering"))
        )

        val krediteringFakturaserie = fakturaserieGenerator.lagFakturaserieKansellering(
            fakturaserieDto,
            startDato,
            sluttDato,
            avregningBehandler.lagAvregningsfakturaer(
                fakturaserieDto.perioder,
                eksisterendeFakturaserie.bestilteFakturaer()
            )
        )

        fakturaserieRepository.save(krediteringFakturaserie)

        eksisterendeFakturaserie.kansellerMed(krediteringFakturaserie)
        fakturaserieRepository.save(eksisterendeFakturaserie)

        fakturaBestillingService.bestillKreditnota(krediteringFakturaserie)
        return krediteringFakturaserie.referanse
    }

}
