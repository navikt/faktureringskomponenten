package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.faktureringskomponenten.controller.validators.ErFodselsnummer

@Schema(description = "Fullmektig som mottar faktura")
data class FullmektigDto(

    @Schema(description = "Fødselsnummer eller d-nummer for fullmektig, 11 siffer")
    @ErFodselsnummer
    val fodselsnummer: String?,

    @Schema(description = "Organisasjonsnummer for fullmektig, 9 siffer")
    val organisasjonsnummer: String?,
)
