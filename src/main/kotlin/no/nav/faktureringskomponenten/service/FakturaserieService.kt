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
        fakturaserieRepository.findByVedtaksId(fakturaserieDto.vedtaksId)
            .ifPresent { throw IllegalArgumentException("Kan ikke opprette fakturaserie når vedtaksId allerede finnes") }

        val fakturaserie = fakturaserieMapper.tilEntitet(fakturaserieDto)
        fakturaserieRepository.save(fakturaserie)

        return fakturaserie
    }
}