package no.nav.faktureringskomponenten.service.integration.kafka.dto

import java.time.LocalDate

data class ManglendeFakturabetalingDto(
    var behandlingId: String,
    var mottaksDato: LocalDate
)