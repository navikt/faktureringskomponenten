package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.mappers.FakturaserieMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Service
class FakturaserieService(
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaserieMapper: FakturaserieMapper,
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
            endreFakturaserie(forrigeReferanse, fakturaserieDto);
            log.info("Kansellerer fakturaserie: ${fakturaserieDto.fakturaserieReferanse}, lager ny fakturaserie: ${fakturaserieDto.fakturaserieReferanse}")
            return fakturaserieDto.fakturaserieReferanse
        }

        val fakturaserie = fakturaserieMapper.tilFakturaserie(fakturaserieDto)
        fakturaserieRepository.save(fakturaserie)
        log.info("Lagret fakturaserie: $fakturaserie")
        return fakturaserie.referanse
    }

    @Transactional
    fun endreFakturaserie(opprinneligReferanse: String, fakturaserieDto: FakturaserieDto) {
        val opprinneligFakturaserie = fakturaserieRepository.findByReferanse(opprinneligReferanse)
            ?: throw RessursIkkeFunnetException(
                field = "referanse",
                message = "Fant ikke opprinnelig fakturaserie med referanse $opprinneligReferanse"
            )

        val opprinneligFakturaserieErUnderBestilling =
            opprinneligFakturaserie.status == FakturaserieStatus.UNDER_BESTILLING

        val fakturaSomIkkeErSendt = opprinneligFakturaserie.faktura.filter { it.status == FakturaStatus.OPPRETTET }
            .sortedBy { it.getPeriodeFra() }

        val fakturaSomIkkeErSendtPeriodeFra =
            if (fakturaSomIkkeErSendt.isNotEmpty()) fakturaSomIkkeErSendt[0].getPeriodeFra() else null

        val nyFakturaserie =
            fakturaserieMapper.tilFakturaserie(
                fakturaserieDto,
                if (opprinneligFakturaserieErUnderBestilling) fakturaSomIkkeErSendtPeriodeFra else null
            )

        opprinneligFakturaserie.status = FakturaserieStatus.ERSTATTET
        fakturaSomIkkeErSendt.forEach { it.status = FakturaStatus.KANSELLERT }

        nyFakturaserie.apply { erstattetMed = opprinneligFakturaserie }

        fakturaserieRepository.save(opprinneligFakturaserie)
        fakturaserieRepository.save(nyFakturaserie)
        log.info("Kansellert fakturaserie med id: ${opprinneligFakturaserie.referanse}, lager ny med id: ${nyFakturaserie.referanse}")
    }

    fun finnesReferanse(referanse: String): Boolean {
        return fakturaserieRepository.findByReferanse(referanse) != null
    }
}
