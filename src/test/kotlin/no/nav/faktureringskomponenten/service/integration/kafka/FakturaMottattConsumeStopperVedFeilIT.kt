package no.nav.faktureringskomponenten.service.integration.kafka

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

@ActiveProfiles("itest", "embeded-kafka")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
class FakturaMottattConsumeStopperVedFeilIT(
    @Autowired @Qualifier("fakturaMottatt") private val kafkaTemplate: KafkaTemplate<String, FakturaMottattDto>,
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired private val fakturaMottakFeilRepository: FakturaMottakFeilRepository,
    @Autowired private val fakturaMottattConsumer: FakturaMottattConsumer
 ) : EmbeddedKafkaBase(fakturaserieRepository) {


    @Test // Kan kun være denne testen i klassen siden offset vil stå på den feilede meldingen etter kjøring
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
}