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


@Component
class FakturaserieService(
    @Autowired val fakturaserieRepository: FakturaserieRepository,
    @Autowired val fakturaserieMapper: FakturaserieMapper
) {

    fun lagNyFakturaserie(fakturaserieDto: FakturaserieDto): Fakturaserie {
        val fakturaserie = fakturaserieMapper.tilFakturaserie(fakturaserieDto)
        fakturaserieRepository.save(fakturaserie)

        return fakturaserie
    }

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

    fun endreFakturaserie(opprinneligVedtaksId: String, fakturaserieDto: FakturaserieDto): Fakturaserie? {
        val opprinneligFakturaserieOptional = fakturaserieRepository.findByVedtaksId(opprinneligVedtaksId)

        if (!opprinneligFakturaserieOptional.isPresent) {
            throw RessursIkkeFunnetException(
                "vedtaksId",
                "Fant ikke opprinnelig fakturaserie med vedtaksId $opprinneligVedtaksId"
            )
        }

        val opprinneligFakturaserie = opprinneligFakturaserieOptional.get()
        val fakturaSomIkkeErSendt = opprinneligFakturaserie.faktura.filter { it.status == FakturaStatus.OPPRETTET }

        val fakturaSomIkkeErSendtPeriodeFra =
            fakturaSomIkkeErSendt.sortedBy { it.getPeriodeFra() }.get(0).getPeriodeFra()

        val nyFakturaserie =
            fakturaserieMapper.tilFakturaserie(fakturaserieDto, fakturaSomIkkeErSendtPeriodeFra)

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