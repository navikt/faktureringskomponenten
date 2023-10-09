package no.nav.faktureringskomponenten.service.integration.kafka.config

import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import org.apache.kafka.common.serialization.Deserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.stereotype.Component

@Component
class DeserializerJsonAware : Deserializer<EksternFakturaStatusDto> {

    private val delegate = JsonDeserializer(EksternFakturaStatusDto::class.java, false)

    var json: String? = null

    override fun deserialize(topic: String, data: ByteArray?): EksternFakturaStatusDto {
        json = data?.let { String(it, Charsets.UTF_8) }
        return delegate.deserialize(topic, data)
    }
}
