package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "Status for fakturaen"
)
enum class FakturaStatus {
    OPPRETTET,
    BESTILT,
    KANSELLERT,
    BETALT,
    DELVIS_BETALT,
    FEIL,
    INNE_I_OEBS,
    MANGLENDE_INNBETALING
}

