package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "DTO for endring av fakturamottaker")
data class FakturamottakerRequestDto(
     val fullmektig: FullmektigDto?,
)