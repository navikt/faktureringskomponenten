package no.nav.faktureringskomponenten.controller.dto

import no.nav.faktureringskomponenten.validators.ErFodselsnummer

data class FullmektigDto(

    @field:ErFodselsnummer
    val fodselsnummer: String?,

    val organisasjonsnummer: String?,

    val kontaktperson: String?,
)
