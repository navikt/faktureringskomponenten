package no.nav.faktureringskomponenten.service.integration.kafka

import tools.jackson.databind.json.JsonMapper
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JacksonJsonSerializer

@TestConfiguration
class KafkaTestConfig {
    @Bean
    @Qualifier("fakturaMottatt")
    fun fakturaMottattKafkaTemplate(
        kafkaProperties: KafkaProperties,
        jsonMapper: JsonMapper
    ): KafkaTemplate<String, EksternFakturaStatusDto> {
        val props = kafkaProperties.buildProducerProperties()
        val producerFactory: ProducerFactory<String, EksternFakturaStatusDto> =
            DefaultKafkaProducerFactory(props, StringSerializer(), JacksonJsonSerializer(jsonMapper))
        return KafkaTemplate(producerFactory)
    }
}
