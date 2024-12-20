package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Betalingsintervall",
    example = "KVARTAL",
)
enum class FakturaserieIntervall {
    MANEDLIG,
    KVARTAL,
    SINGEL
}