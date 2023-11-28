package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.avregning.AvregningBehandler
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Service
class FakturaserieService(
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaserieGenerator: FakturaserieGenerator,
    private val avregningBehandler: AvregningBehandler,
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

        val fakturaserie = fakturaserieGenerator.lagFakturaserie(fakturaserieDto, avregningsfaktura = null)
        fakturaserieRepository.save(fakturaserie)
        log.info("Lagret fakturaserie: $fakturaserie")
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
            avregningBehandler.lagAvregningsfaktura(
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

    fun kansellerFakturaserie(referanse: String) {
        val fakturaserie = fakturaserieRepository.findByReferanse(referanse)
            ?: throw IllegalArgumentException("Fakturaserie med referanse $referanse finnes ikke")

        val krediteringFakturaserie = fakturaserieGenerator.lagKrediteringFakturaSerie(fakturaserie)
        fakturaserieRepository.save(krediteringFakturaserie)

        fakturaserie.apply {
            status = FakturaserieStatus.KANSELLERT
            faktura.apply {
                status = FakturaserieStatus.KANSELLERT
            }
        }

        fakturaserieRepository.save(fakturaserie)
    }
}
