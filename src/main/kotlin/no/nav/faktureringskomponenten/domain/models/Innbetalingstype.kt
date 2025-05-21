package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Faktura gjelder innbetaling av")
enum class Innbetalingstype {
    TRYGDEAVGIFT,
    AARSAVREGNING
}
