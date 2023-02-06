package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.mappers.FakturaserieMapper
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
class FakturaserieService(
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaserieMapper: FakturaserieMapper,
) {

    private val log: Logger = LoggerFactory.getLogger(FakturaserieService::class.java)

    fun hentFakturaserie(vedtaksId: String): Fakturaserie =
        fakturaserieRepository.findByVedtaksId(vedtaksId) ?: throw RessursIkkeFunnetException(
            field = "vedtaksId",
            message = "Fant ikke fakturaserie på: $vedtaksId"
        )

    @Transactional
    fun lagNyFakturaserie(fakturaserieDto: FakturaserieDto): Fakturaserie {
        val fakturaserie = fakturaserieMapper.tilFakturaserie(fakturaserieDto)
        log.info("Mottatt $fakturaserieDto")

        fakturaserieRepository.save(fakturaserie)
        log.info("Lagret fakturaserie: $fakturaserie")
        return fakturaserie
    }

    @Transactional
    fun endreFakturaserie(opprinneligVedtaksId: String, fakturaserieDto: FakturaserieDto): Fakturaserie? {
        val opprinneligFakturaserie = fakturaserieRepository.findByVedtaksId(opprinneligVedtaksId)
            ?: throw RessursIkkeFunnetException(
                field = "vedtaksId",
                message = "Fant ikke opprinnelig fakturaserie med vedtaksId $opprinneligVedtaksId"
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

        opprinneligFakturaserie.status = FakturaserieStatus.KANSELLERT
        fakturaSomIkkeErSendt.forEach { it.status = FakturaStatus.KANSELLERT }

        fakturaserieRepository.save(opprinneligFakturaserie)
        fakturaserieRepository.save(nyFakturaserie)

        return nyFakturaserie
    }

    fun finnesVedtaksId(vedtaksId: String): Boolean {
        return fakturaserieRepository.findByVedtaksId(vedtaksId) != null
    }
}
