package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.mappers.FakturaserieMapper
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

    fun finnesVedtaksId(vedtaksId: String): Boolean {
        return fakturaserieRepository.findByVedtaksId(vedtaksId).isPresent
    }
}