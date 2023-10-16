package no.nav.faktureringskomponenten.service.integration.kafka

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit

@ActiveProfiles("itest", "embeded-kafka")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
class EksternFakturaStatusConsumeStopperVedFeilIT(
    @Autowired @Qualifier("fakturaMottatt") private val kafkaTemplate: KafkaTemplate<String, EksternFakturaStatusDto>,
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired private val fakturaMottakFeilRepository: FakturaMottakFeilRepository,
    @Autowired private val eksternFakturaStatusConsumer: EksternFakturaStatusConsumer
) : EmbeddedKafkaBase(fakturaserieRepository) {

    @Test // Kan kun være denne testen i klassen siden offset vil stå på den feilede meldingen etter kjøring
    fun `les faktura fra kafka kø skal stoppe ved feil og ikke avansere offset`() {
        val fakturaReferanseNr = UUID.randomUUID().toString()
        val (_, faktura) = (1..2).map {
            lagFakturaMedSerie(
                faktura = Faktura(status = if (it == 1) FakturaStatus.OPPRETTET else FakturaStatus.BESTILLT, referanseNr = fakturaReferanseNr),
                referanse = "MEL-$it-$it"
            ).apply {
                kafkaTemplate.send(
                    kafkaTopic, EksternFakturaStatusDto(
                        fakturaReferanseNr = "$fakturaReferanseNr 123",
                        fakturaNummer = "82",
                        dato = LocalDate.now(),
                        status = FakturaStatus.INNE_I_OEBS,
                        fakturaBelop = BigDecimal(1000),
                        ubetaltBelop = BigDecimal(2000),
                        feilmelding = null
                    )
                )
            }
        }

        await
            .timeout(30, TimeUnit.SECONDS)
            .until {
                fakturaMottakFeilRepository.findAll().isNotEmpty()
            }

        fakturaMottakFeilRepository.findAll()
            .shouldHaveSize(1)
            .first().apply {
                error.shouldStartWith("Finner ikke faktura med faktura referanse nr $fakturaReferanseNr")
                kafkaOffset.shouldBe(0)
            }

        val listenerContainer = eksternFakturaStatusConsumer.EksternFakturaStatusListenerContainer()
        await.timeout(20, TimeUnit.SECONDS).until { !listenerContainer.isRunning }

        await.timeout(5, TimeUnit.SECONDS).until {
            if (!listenerContainer.isRunning) {
                // fungerer ikke å starte på utsiden av await
                eksternFakturaStatusConsumer.EksternFakturaStatusListenerContainer().start()
            }
            listenerContainer.isRunning
        }

        await.timeout(10, TimeUnit.SECONDS).until {
            fakturaMottakFeilRepository.findAll().size == 2
        }
        // FakturaStatus blir BETALT om neste kafka melding blir prosessert
//        fakturaRepository.findByReferanseNr(faktura.id.toString())?.status.shouldBe(FakturaStatus.BESTILLT)
    }
}
