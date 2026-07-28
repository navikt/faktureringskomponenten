package no.nav.faktureringskomponenten.service.integration.kafka.config

import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper


class FakturaBestiltSerializer : Serializer<FakturaBestiltDto> {
    private val objectMapper = JsonMapper.builder()
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()

    override fun serialize(topic: String?, data: FakturaBestiltDto?): ByteArray? {
        return objectMapper.writeValueAsBytes(
            data ?: throw SerializationException("Error ved serializing av FakturaBestiltDto til ByteArray[]")
        )
    }

    override fun close() {}
}