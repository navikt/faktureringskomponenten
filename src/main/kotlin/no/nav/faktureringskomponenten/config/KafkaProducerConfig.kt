package no.nav.faktureringskomponenten.config

import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import java.util.*

@Configuration
@EnableKafka
class KafkaProducerConfig(
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

    private fun commonProps(): Map<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[CommonClientConfigs.CLIENT_ID_CONFIG] = "melosys-producer"
        props[ProducerConfig.ACKS_CONFIG] = "all"
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokersUrl
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = FakturaBestiltSerializer::class.java
        if (!isLocal) {
            props[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
            props[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = truststorePath
            props[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credstorePassword
            props[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
            props[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = keystorePath
            props[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credstorePassword
            props[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = credstorePassword
            props[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
        }
        return props
    }

    private val isLocal: Boolean
        get() = Arrays.stream(env.activeProfiles).anyMatch { profile: String ->
            profile.equals("local", ignoreCase = true)
        }
}
