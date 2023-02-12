package no.nav.faktureringskomponenten.config

import org.apache.kafka.common.serialization.Deserializer
import org.springframework.kafka.support.serializer.JsonDeserializer

class ErrorHandlingDeserializerFailedJsonAware<T>(private val delegate: JsonDeserializer<T>) : Deserializer<T> {
    private var failedJson: String? = null

    override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
        delegate.configure(configs, isKey)
    }

    override fun deserialize(topic: String, data: ByteArray?): T {
        failedJson = data?.let { String(it, Charsets.UTF_8) }
        return delegate.deserialize(topic, data)
    }

    override fun close() {
        delegate.close()
    }

    fun getFailedJson(): String? {
        return failedJson
    }
}
