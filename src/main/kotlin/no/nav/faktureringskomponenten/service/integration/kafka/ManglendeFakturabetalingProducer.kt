package no.nav.faktureringskomponenten.service.integration.kafka

import no.nav.faktureringskomponenten.service.integration.kafka.dto.ManglendeFakturabetalingDto

fun interface ManglendeFakturabetalingProducer {
    fun produserBestillingsmelding(manglendeFakturabetalingDto: ManglendeFakturabetalingDto)
}
