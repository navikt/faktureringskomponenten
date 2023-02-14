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
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer
import java.util.*


@Configuration
@EnableKafka
class KafkaConfig(
    private val env: Environment,
    @Value("\${kafka.aiven.brokers}") private val brokersUrl: String,
    @Value("\${kafka.aiven.keystorePath}") private val keystorePath: String,
    @Value("\${kafka.aiven.truststorePath}") private val truststorePath: String,
    @Value("\${kafka.aiven.credstorePassword}") private val credstorePassword: String,
) {

    @Bean
    fun producerFactory(): ProducerFactory<String, FakturaBestiltDto> {
        return DefaultKafkaProducerFactory(producerProps())
    }

    @Bean
    @Qualifier("fakturaBestilt")
    fun fakturaBestiltTemplate(): KafkaTemplate<String, FakturaBestiltDto> =
        KafkaTemplate(producerFactory())

    private fun producerProps(): Map<String, Any> = mutableMapOf<String, Any>(
        CommonClientConfigs.CLIENT_ID_CONFIG to "melosys-producer",
        ProducerConfig.ACKS_CONFIG to "all",
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to brokersUrl,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to FakturaBestiltSerializer::class.java
    ) + securityConfig()

    @Bean
    fun faktarMottattHendelseListenerContainerFactory(
        kafkaProperties: KafkaProperties,
        containerStoppingFailedJsonAwareErrorHandler: ContainerStoppingFailedJsonAwareErrorHandler,
        valueDeserializer: DeserializerJsonAware
    ) =
        ConcurrentKafkaListenerContainerFactory<String, FakturaMottattDto>().apply {
            setCommonErrorHandler(containerStoppingFailedJsonAwareErrorHandler)

            consumerFactory = DefaultKafkaConsumerFactory(
                kafkaProperties.buildConsumerProperties() + consumerConfig(),
                StringDeserializer(),
                valueDeserializer
            )
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        }

    private fun consumerConfig(): Map<String, Any> = mapOf<String, Any>(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to brokersUrl,
        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
        ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG to 15000,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
        ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 1
    ) + securityConfig()

    private fun securityConfig(): Map<String, Any> =
        if (isLocal) mapOf() else mapOf<String, Any>(
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to truststorePath,
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to credstorePassword,
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to "JKS",
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to keystorePath,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to credstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to credstorePassword,
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to "PKCS12"
        )

    private val isLocal: Boolean
        get() = env.activeProfiles.any { profile: String ->
            profile.equals("local", ignoreCase = true) || profile.equals("itest", ignoreCase = true)
        }
}
