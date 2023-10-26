package no.nav.faktureringskomponenten.service.integration.kafka.dto

import java.time.LocalDate

data class ManglendeFakturabetalingDto(
    var fakturaserieReferanse: String,
    var betalingstatus: Betalingstatus,
    var mottaksDato: LocalDate
)