package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Tema",
    example = "TRY",
)
enum class FakturaserieTemaDto {
    TRY,
}