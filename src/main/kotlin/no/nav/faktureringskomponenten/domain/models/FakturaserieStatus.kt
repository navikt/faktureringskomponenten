package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Status for en fakturaserie"
)
enum class FakturaserieStatus {
    OPPRETTET,
    UNDER_BESTILLING,
    KANSELLERT,
    ERSTATTET,
    FERDIG
}