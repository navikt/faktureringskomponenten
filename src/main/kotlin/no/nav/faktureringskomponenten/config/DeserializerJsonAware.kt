package no.nav.faktureringskomponenten.config

import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import org.apache.kafka.common.serialization.Deserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.stereotype.Component

@Component
class DeserializerJsonAware : Deserializer<FakturaMottattDto> {

    private val delegate = JsonDeserializer(FakturaMottattDto::class.java, false)

    var json: String? = null

    override fun deserialize(topic: String, data: ByteArray?): FakturaMottattDto {
        json = data?.let { String(it, Charsets.UTF_8) }
        return delegate.deserialize(topic, data)
    }
}