package no.nav.faktureringskomponenten.service.integration.kafka.dto

import java.time.LocalDate

data class ManglendeFakturabetalingDto(
    var fakturaserieReferanse: String,
    var mottaksDato: LocalDate
)