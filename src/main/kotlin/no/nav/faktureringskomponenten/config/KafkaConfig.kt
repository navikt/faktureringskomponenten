package no.nav.faktureringskomponenten.config

import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import java.util.*

@Configuration
@EnableKafka
class KafkaConfig(
    private val env: Environment,
    @Value("\${kafka.aiven.brokers}") private val brokersUrl: String,
    @Value("\${kafka.aiven.keystorePath}") private val keystorePath: String,
    @Value("\${kafka.aiven.truststorePath}") private val truststorePath: String,
    @Value("\${kafka.aiven.credstorePassword}") private val credstorePassword: String
) {

    @Bean
    fun producerFactory(): ProducerFactory<String, FakturaBestiltDto> {
        return DefaultKafkaProducerFactory(commonProps())
    }

    @Bean
    @Qualifier("fakturaBestilt")
    fun fakturaBestiltTemplate(): KafkaTemplate<String, FakturaBestiltDto> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    fun faktarMottattHendelseListenerContainerFactory(
        kafkaProperties: KafkaProperties
    ): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String?, FakturaMottattDto?>?>? {
        return fakturaMottattListenerContainerFactory(kafkaProperties)
    }

    private fun fakturaMottattListenerContainerFactory(kafkaProperties: KafkaProperties): ConcurrentKafkaListenerContainerFactory<String, FakturaMottattDto>? {
        val props = kafkaProperties.buildConsumerProperties()
        props.putAll(consumerConfig())
        val defaultKafkaConsumerFactory = DefaultKafkaConsumerFactory<String, FakturaMottattDto>(
            props, StringDeserializer(), valueDeserializer(FakturaMottattDto::class.java)
        )
        val factory: ConcurrentKafkaListenerContainerFactory<String, FakturaMottattDto> =
            ConcurrentKafkaListenerContainerFactory<String, FakturaMottattDto>()
        factory.setConsumerFactory(defaultKafkaConsumerFactory)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        return factory
    }

    private fun consumerConfig(): Map<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokersUrl
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = false
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG] = 15000
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = JsonDeserializer::class.java
        props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 1
        if (!isLocal) {
            props.putAll(securityConfig())
        }
        return props
    }

    private fun securityConfig(props: MutableMap<String, Any> = HashMap()): Map<String, Any> {
        props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
        props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = truststorePath
        props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credstorePassword
        props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
        props[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = keystorePath
        props[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credstorePassword
        props[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = credstorePassword
        props[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
        return props
    }

    private fun <T> valueDeserializer(targetType: Class<T>): ErrorHandlingDeserializer<T>? {
        return ErrorHandlingDeserializer(JsonDeserializer(targetType, false))
    }

    private fun commonProps(): Map<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[CommonClientConfigs.CLIENT_ID_CONFIG] = "melosys-producer"
        props[ProducerConfig.ACKS_CONFIG] = "all"
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokersUrl
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = FakturaBestiltSerializer::class.java
        if (!isLocal) {
            securityConfig(props)
        }
        return props
    }

    private val isLocal: Boolean
        get() = Arrays.stream(env.activeProfiles).anyMatch { profile: String ->
            profile.equals("local", ignoreCase = true)
        }
}
