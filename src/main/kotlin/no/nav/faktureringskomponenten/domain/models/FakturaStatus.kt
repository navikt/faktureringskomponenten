package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Status for fakturaen"
)
enum class FakturaStatus {
    OPPRETTET,
    BESTILLT,
    KANSELLERT,
    BETALT,
    DELVIS_BETALT,
}