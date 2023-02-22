package no.nav.faktureringskomponenten.service.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@TestConfiguration
class KafkaTestConfig {
    @Bean
    @Qualifier("fakturaMottatt")
    fun fakturaMottattKafkaTemplate(
        kafkaProperties: KafkaProperties,
        objectMapper: ObjectMapper?
    ): KafkaTemplate<String, FakturaMottattDto> {
        val props = kafkaProperties.buildProducerProperties()
        val producerFactory: ProducerFactory<String, FakturaMottattDto> =
            DefaultKafkaProducerFactory(props, StringSerializer(), JsonSerializer(objectMapper))
        return KafkaTemplate(producerFactory)
    }
}
