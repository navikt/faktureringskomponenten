package no.nav.faktureringskomponenten.service.integration.kafka

import no.nav.faktureringskomponenten.domain.models.ErrorTypes

class EksternFakturaStatusConsumerException(
    message: String,
    val offset: Long,
    val errorType: ErrorTypes,
    val fakturaReferanseNr: String,
    cause: Throwable?
) : RuntimeException(message, cause)
