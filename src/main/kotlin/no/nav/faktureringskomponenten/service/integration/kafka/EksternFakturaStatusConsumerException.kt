package no.nav.faktureringskomponenten.service.integration.kafka

class EksternFakturaStatusConsumerException(
    message: String,
    val offset: Long,
    cause: Throwable?
) : RuntimeException(message, cause)
