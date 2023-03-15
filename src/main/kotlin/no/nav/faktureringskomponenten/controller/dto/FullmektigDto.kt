package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.faktureringskomponenten.controller.validators.ErFodselsnummer

@Schema(description = "Fullmektig som mottar faktura")
data class FullmektigDto(

    @Schema(description = "Fødselsnummer for fullmektig, 11 siffer")
    @ErFodselsnummer
    val fodselsnummer: String?,

    @Schema(description = "Organisasjonsnummer for fullmektig")
    val organisasjonsnummer: String?,

    @Schema(
        description = "Navn på kontaktperson hos fullmektig",
        example = "Ola Nordmann"
    )
    val kontaktperson: String?,
)
