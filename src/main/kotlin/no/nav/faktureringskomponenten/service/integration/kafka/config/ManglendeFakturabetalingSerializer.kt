package no.nav.faktureringskomponenten.service.integration.kafka.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.faktureringskomponenten.service.integration.kafka.dto.ManglendeFakturabetalingDto
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer


class ManglendeFakturabetalingSerializer : Serializer<ManglendeFakturabetalingDto> {
    private val objectMapper = ObjectMapper()

    override fun serialize(topic: String?, data: ManglendeFakturabetalingDto?): ByteArray? {
        objectMapper.registerModule(JavaTimeModule())
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        return objectMapper.writeValueAsBytes(
            data ?: throw SerializationException("Error ved serializing av FakturaBestiltDto til ByteArray[]")
        )
    }

    override fun close() {}
}