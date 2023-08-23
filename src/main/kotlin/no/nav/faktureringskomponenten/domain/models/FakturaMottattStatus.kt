package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Status for faktura mottatt")
enum class FakturaMottattStatus {
    MANGLENDE_INNBETALING,
    INNE_I_OEBS,
    FEIL
}