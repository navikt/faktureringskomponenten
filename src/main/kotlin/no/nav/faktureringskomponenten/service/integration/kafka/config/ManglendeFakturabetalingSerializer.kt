package no.nav.faktureringskomponenten.service.integration.kafka.config

import no.nav.faktureringskomponenten.service.integration.kafka.dto.ManglendeFakturabetalingDto
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper


class ManglendeFakturabetalingSerializer : Serializer<ManglendeFakturabetalingDto> {
    private val objectMapper = JsonMapper.builder()
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()

    override fun serialize(topic: String?, data: ManglendeFakturabetalingDto?): ByteArray? {
        return objectMapper.writeValueAsBytes(
            data ?: throw SerializationException("Error ved serializing av ManglendeFakturabetalingDto til ByteArray[]")
        )
    }

    override fun close() {}
}