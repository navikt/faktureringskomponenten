package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Service
class FakturaserieService(
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaserieGenerator: FakturaserieGenerator,
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
        if(!forrigeReferanse.isNullOrEmpty()){
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
        check(opprinneligFakturaserie.erAktiv()) { "Bare aktiv fakturaserie kan erstattes"}

        val nyFakturaserie = fakturaserieGenerator.lagFakturaserie(
            fakturaserieDto,
            if (opprinneligFakturaserie.erUnderBestilling()) {
                opprinneligFakturaserie.planlagteFakturaer().minByOrNull { it.getPeriodeFra() }?.getPeriodeFra()
            } else null,
            lagAvregningsFaktura(listOf(opprinneligFakturaserie.faktura[0]))
        )
        fakturaserieRepository.save(nyFakturaserie)

        opprinneligFakturaserie.erstattMed(nyFakturaserie)
        fakturaserieRepository.save(opprinneligFakturaserie)

        log.info("Kansellert fakturaserie: ${opprinneligFakturaserie.referanse}, lagret ny: ${nyFakturaserie.referanse}")
        return nyFakturaserie.referanse
    }
    private fun lagAvregningsFaktura(fakturaerBruktTilAvregning: List<Faktura>): Faktura {
        val linje = FakturaLinje(
            referertFakturaVedAvregning = fakturaerBruktTilAvregning[0],
            periodeFra = LocalDate.of(2024, 1, 1),
            periodeTil = LocalDate.of(2024, 3, 31),
            beskrivelse = "it.beskrivelse",
            antall = BigDecimal(1),
            enhetsprisPerManed = BigDecimal(1000),
            belop = BigDecimal(1000),
        )
        return Faktura(null, LocalDate.now(), fakturaLinje = listOf(linje))
    }

    fun finnesReferanse(referanse: String): Boolean {
        return fakturaserieRepository.findByReferanse(referanse) != null
    }
}
