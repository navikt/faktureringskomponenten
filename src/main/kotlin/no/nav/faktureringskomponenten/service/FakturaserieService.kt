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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional


@Component
class FakturaserieService(
    @Autowired val fakturaserieRepository: FakturaserieRepository,
    @Autowired val fakturaserieMapper: FakturaserieMapper,
    @Autowired val fakturaService: FakturaService
) {

    private val log: Logger = LoggerFactory.getLogger(FakturaserieService::class.java)

    fun hentFakturaserie(vedtaksId: String): Fakturaserie {
        val fakturaserie = fakturaserieRepository.findByVedtaksId(vedtaksId)

        if (!fakturaserie.isPresent) {
            throw RessursIkkeFunnetException(
                felt = "vedtaksId",
                melding = "Fant ikke fakturaserie på: $vedtaksId"
            )
        }

        return fakturaserie.get()
    }

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
        val opprinneligFakturaserieOptional = fakturaserieRepository.findByVedtaksId(opprinneligVedtaksId)

        if (!opprinneligFakturaserieOptional.isPresent) {
            throw RessursIkkeFunnetException(
                felt = "vedtaksId",
                melding = "Fant ikke opprinnelig fakturaserie med vedtaksId $opprinneligVedtaksId"
            )
        }

        val opprinneligFakturaserie = opprinneligFakturaserieOptional.get()

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
        return fakturaserieRepository.findByVedtaksId(vedtaksId).isPresent
    }
}
