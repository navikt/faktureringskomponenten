package no.nav.faktureringskomponenten.config

import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import org.apache.kafka.common.serialization.Deserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.stereotype.Component

@Component
class DeserializerFailedJsonAware : Deserializer<FakturaMottattDto> {

    private val delegate = JsonDeserializer(FakturaMottattDto::class.java, false)

    private var failedJson: String? = null

    override fun deserialize(topic: String, data: ByteArray?): FakturaMottattDto {
        failedJson = data?.let { String(it, Charsets.UTF_8) }
        return delegate.deserialize(topic, data)
    }

    fun getFailedJson(): String? {
        return failedJson
    }
}
