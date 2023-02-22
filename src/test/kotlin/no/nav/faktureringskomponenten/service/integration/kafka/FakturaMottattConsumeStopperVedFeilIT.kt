package no.nav.faktureringskomponenten.service.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.common.serialization.StringSerializer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@ActiveProfiles("itest", "embeded-kafka")
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["faktura-mottatt-topic-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
class FakturaMottattConsumeStopperVedFeilIT(
    @Autowired @Qualifier("fakturaMottatt") private val kafkaTemplate: KafkaTemplate<String, FakturaMottattDto>,
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired private val fakturaMottakFeilRepository: FakturaMottakFeilRepository,
    @Autowired private val fakturaMottattConsumer: FakturaMottattConsumer
) : PostgresTestContainerBase() {
    private val kafkaTopic = "faktura-mottatt-topic-local"

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

    @Test
    fun `les faktura fra kafka kø skal stoppe ved feil og ikke avansere offset`() {
        val (f1, f2) = (1..2).map {
            val faktura = lagFakturaMedSerie(
                faktura = Faktura(status = if (it == 1) FakturaStatus.OPPRETTET else FakturaStatus.BESTILLT),
                vedtaksId = "MEL-$it-$it"
            )
            kafkaTemplate.send(
                kafkaTopic, FakturaMottattDto(
                    fodselsnummer = "12345678901",
                    vedtaksId = "MEL-$it-$it",
                    fakturaReferanseNr = faktura.id.toString(),
                    kreditReferanseNr = "",
                    belop = BigDecimal(1000),
                    status = FakturaStatus.BETALT
                )
            )
            faktura
        }

        await
            .timeout(30, TimeUnit.SECONDS)
            .until {
                fakturaMottakFeilRepository.findAll().isNotEmpty()
            }

        fakturaMottakFeilRepository.findAll()
            .shouldHaveSize(1)
            .first().apply {
                error.shouldStartWith("Faktura melding mottatt fra oebs med status: OPPRETTET")
                //kafkaOffset.shouldBe(1) må finne en måte å hent ut dette far consumer
            }

        val listenerContainer = fakturaMottattConsumer.fakturaMottattListenerContainer()
        await.timeout(20, TimeUnit.SECONDS).until { !listenerContainer.isRunning }

        await.timeout(5, TimeUnit.SECONDS).until {
            if (!listenerContainer.isRunning) {
                // fungerer ikke å starte på utsiden av await
                fakturaMottattConsumer.start()
            }
            listenerContainer.isRunning
        }

        await.timeout(10, TimeUnit.SECONDS).until {
            fakturaMottakFeilRepository.findAll().size == 2
        }
        // FakturaStatus blir BETALT om neste kafka melding blir prosessert
        fakturaRepository.findById(f2.id!!)?.status.shouldBe(FakturaStatus.BESTILLT)

        fakturaserieRepository.delete(f1.fakturaserie!!)
        fakturaserieRepository.delete(f2.fakturaserie!!)
    }


    private fun lagFakturaMedSerie(faktura: Faktura, vedtaksId: String = "MEL-1-1"): Faktura =
        fakturaserieRepository.saveAndFlush(
            Fakturaserie(
                vedtaksId = vedtaksId,
                fodselsnummer = "01234567890",
                faktura = mutableListOf(faktura)
            ).apply { this.faktura.forEach { it.fakturaserie = this } }
        ).faktura.first()
}
