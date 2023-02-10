package no.nav.faktureringskomponenten.service.integration.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
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

@ActiveProfiles(value = ["itest", "embeded-kafka"])
@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["faktura-mottatt-topic-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@SpringBootTest()
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
class FakturaMottattConsumerIT(
    @Autowired @Qualifier("fakturaBestilt") private val kafkaTemplate: KafkaTemplate<String, FakturaMottattDto>,
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
) : PostgresTestContainerBase() {
    private val kafkaTopic = "faktura-mottatt-topic-local"

    @TestConfiguration
    class KafkaTestConfig {
        @Bean
        @Qualifier("fakturaBestilt")
        fun melosysEessiMeldingKafkaTemplate(
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
    fun `les faktura fra kakfak kø og sjekk at status blir satt til BETALT`() {
        val faktura = lagFakturaMedSerie(
            Faktura(
                status = FakturaStatus.BESTILLT,
            )
        )
        val fakturaMottattDto = FakturaMottattDto(
            fodselsnummer = "12345678901",
            vedtaksId = "MEL-1-1",
            fakturaReferanseNr = faktura.id.toString(),
            kreditReferanseNr = "",
            belop = BigDecimal(1000),
            status = FakturaStatus.BETALT
        )


        kafkaTemplate.send(kafkaTopic, fakturaMottattDto)


        await.timeout(20, TimeUnit.SECONDS)
            .until {
                fakturaRepository.findById(faktura.id!!)!!.status == FakturaStatus.BETALT
            }

        fakturaserieRepository.delete(faktura.fakturaserie!!)
    }

    private fun lagFakturaMedSerie(faktura: Faktura): Faktura =
        fakturaserieRepository.saveAndFlush(
            Fakturaserie(
                vedtaksId = "MEL-1-1",
                fodselsnummer = "01234567890",
                faktura = mutableListOf(faktura)
            ).apply { this.faktura.forEach { it.fakturaserie = this } }
        ).faktura.first()
}
