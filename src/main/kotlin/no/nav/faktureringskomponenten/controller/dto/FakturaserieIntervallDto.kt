package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Betalingsintervall",
    example = "KVARTAL",
)
enum class FakturaserieIntervallDto {
    KVARTAL,
    MANEDLIG
}