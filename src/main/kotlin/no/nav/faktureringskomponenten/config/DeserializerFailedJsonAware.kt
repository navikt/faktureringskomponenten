package no.nav.faktureringskomponenten.config

import org.apache.kafka.common.serialization.Deserializer
import org.springframework.kafka.support.serializer.JsonDeserializer

class DeserializerFailedJsonAware<T>(private val delegate: JsonDeserializer<T>) : Deserializer<T> {
    private var failedJson: String? = null

    override fun deserialize(topic: String, data: ByteArray?): T {
        failedJson = data?.let { String(it, Charsets.UTF_8) }
        return delegate.deserialize(topic, data)
    }

    fun getFailedJson(): String? {
        return failedJson
    }
}
