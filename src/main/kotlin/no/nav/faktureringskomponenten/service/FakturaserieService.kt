package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

    fun hentFakturaserier(referanse: String, fakturaStatus: String?): List<Fakturaserie> {
        return fakturaserieRepository.findAllByReferanse(referanse, fakturaStatus)
    }

    @Transactional
    fun lagNyFakturaserie(fakturaserieDto: FakturaserieDto, forrigeReferanse: String? = null): String {
        if(!forrigeReferanse.isNullOrEmpty()){
            return erstattFakturaserie(forrigeReferanse, fakturaserieDto)
        }

        val fakturaserie = fakturaserieGenerator.lagFakturaserie(fakturaserieDto)
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

        val tidligereFakturaerTilBestilling = opprinneligFakturaserie.faktura.filter { it.status == FakturaStatus.OPPRETTET }        
        val nyFakturaserie =
            fakturaserieGenerator.lagFakturaserie(
                fakturaserieDto,
                if (opprinneligFakturaserie.status == FakturaserieStatus.UNDER_BESTILLING)
                    if (tidligereFakturaerTilBestilling.isNotEmpty()) tidligereFakturaerTilBestilling.sortedBy { it.getPeriodeFra() }[0].getPeriodeFra() else null
                else null
            )
        fakturaserieRepository.save(nyFakturaserie)

        kansellerFakturarerTilBestilling(opprinneligFakturaserie)
        erstattMed(opprinneligFakturaserie, nyFakturaserie)
        fakturaserieRepository.save(opprinneligFakturaserie)

        log.info("Kansellert fakturaserie: ${opprinneligFakturaserie.referanse}, lagret ny: ${nyFakturaserie.referanse}")
        return nyFakturaserie.referanse
    }

    private fun erstattMed(
        opprinneligFakturaserie: Fakturaserie,
        nyFakturaserie: Fakturaserie
    ) {
        opprinneligFakturaserie.apply { erstattetMed = nyFakturaserie }
        opprinneligFakturaserie.status = FakturaserieStatus.ERSTATTET
    }

    private fun kansellerFakturarerTilBestilling(fakturaserie: Fakturaserie) {
        val fakturaerTilBestilling = fakturaserie.faktura.filter { it.status == FakturaStatus.OPPRETTET }
        fakturaerTilBestilling.forEach { it.status = FakturaStatus.KANSELLERT }
        fakturaserieRepository.save(fakturaserie)
    }

    fun finnesReferanse(referanse: String): Boolean {
        return fakturaserieRepository.findByReferanse(referanse) != null
    }
}
