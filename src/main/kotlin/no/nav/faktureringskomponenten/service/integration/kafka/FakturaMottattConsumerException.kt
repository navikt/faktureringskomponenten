package no.nav.faktureringskomponenten.service.integration.kafka

class FakturaMottattConsumerException(
    message: String,
    val offset: Long,
    cause: Throwable?
) : RuntimeException(message, cause)
