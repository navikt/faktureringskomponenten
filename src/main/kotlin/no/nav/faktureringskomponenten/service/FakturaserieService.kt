package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import org.springframework.stereotype.Component

@Component
class FakturaserieService {

    fun lagNyFaktura(fakturaserie: FakturaserieDto): String {
        return "Implementeres i https://jira.adeo.no/browse/MELOSYS-5501, $fakturaserie"
    }
}