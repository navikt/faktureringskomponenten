package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.mappers.FakturaserieMapper
import no.nav.faktureringskomponenten.validators.RessursIkkeFunnetException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate


@Component
class FakturaserieService(
    @Autowired val fakturaserieRepository: FakturaserieRepository,
    @Autowired val fakturaserieMapper: FakturaserieMapper,
    @Autowired val fakturaService: FakturaService
) {
    fun hentFakturaserie(vedtaksId: String): Fakturaserie {
        val fakturaserie = fakturaserieRepository.findByVedtaksId(vedtaksId)

        if (!fakturaserie.isPresent) {
            throw RessursIkkeFunnetException(
                "vedtaksId",
                "Fant ikke fakturaserie på: $vedtaksId"
            )
        }

        return fakturaserie.get()
    }

    @Transactional
    fun lagNyFakturaserie(fakturaserieDto: FakturaserieDto): Fakturaserie {
        val fakturaserie = fakturaserieMapper.tilFakturaserie(fakturaserieDto)
        fakturaserieRepository.save(fakturaserie)

        return fakturaserie
    }

    @Transactional
    fun endreFakturaserie(opprinneligVedtaksId: String, fakturaserieDto: FakturaserieDto): Fakturaserie? {
        val opprinneligFakturaserieOptional = fakturaserieRepository.findByVedtaksId(opprinneligVedtaksId)

        if (!opprinneligFakturaserieOptional.isPresent) {
            throw RessursIkkeFunnetException(
                "vedtaksId",
                "Fant ikke opprinnelig fakturaserie med vedtaksId $opprinneligVedtaksId"
            )
        }

        val opprinneligFakturaserie = opprinneligFakturaserieOptional.get()

        val opprinneligFakturaserieErUnderBestilling =
            opprinneligFakturaserie.status == FakturaserieStatus.UNDER_BESTILLING

        val fakturaSomIkkeErSendt = opprinneligFakturaserie.faktura.filter { it.status == FakturaStatus.OPPRETTET }
        val fakturaSomIkkeErSendtPeriodeFra =
            fakturaSomIkkeErSendt.sortedBy { it.getPeriodeFra() }.get(0).getPeriodeFra()


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

    @Transactional
    fun bestillFakturaserie(vedtaksId: String, bestillingsDato: LocalDate? = LocalDate.now()) {
        val fakturaserie = fakturaserieRepository.findByVedtaksId(vedtaksId).get()
        fakturaserie.apply { status = FakturaserieStatus.UNDER_BESTILLING }

        fakturaserie.faktura
            .filter { it.datoBestilt <= bestillingsDato && it.status == FakturaStatus.OPPRETTET }
            .forEach {
                it.id?.let { fakturaId -> fakturaService.bestillFaktura_gammel(fakturaId) }
            }

        fakturaserieRepository.save(fakturaserie)
    }

    fun finnesVedtaksId(vedtaksId: String): Boolean {
        return fakturaserieRepository.findByVedtaksId(vedtaksId).isPresent
    }
}