package no.nav.faktureringskomponenten.service.integration.kafka

import io.kotest.matchers.bigdecimal.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.EksternFakturaStatusRepository
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
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@ActiveProfiles("itest", "embeded-kafka")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
class EksternFakturaStatusConsumerIT(
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired @Qualifier("fakturaMottatt") private var kafkaTemplate: KafkaTemplate<String, EksternFakturaStatusDto>
) : EmbeddedKafkaBase(fakturaserieRepository) {

    @Test
    fun `les faktura fra kafka kø og lagre melding fra OEBS i DB`(){
        val faktura = lagFakturaMedSerie(
            Faktura()
        )
        val eksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = faktura.id.toString(),
            fakturaNummer = "82",
            dato = LocalDate.now(),
            status = FakturaStatus.INNE_I_OEBS,
            fakturaBelop = BigDecimal(4000),
            ubetaltBelop = BigDecimal(2000),
            feilmelding = null
        )

        kafkaTemplate.send(kafkaTopic, eksternFakturaStatusDto)

        await.timeout(20, TimeUnit.SECONDS)
            .until {
                fakturaRepository.findById(faktura.id!!)?.eksternFakturaStatus?.isNotEmpty()
            }
    }

    @Test
    fun `les faktura fra kafka kø og lagre melding fra OEBS i DB, sjekk feilmelding finnes`(){
        val faktura = lagFakturaMedSerie(
            Faktura()
        )
        val eksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = faktura.id.toString(),
            fakturaNummer = "82",
            dato = LocalDate.now(),
            status = FakturaStatus.FEIL,
            fakturaBelop = BigDecimal(4000),
            ubetaltBelop = BigDecimal(2000),
            feilmelding = "Feil med faktura, mangler faktura referanse nummer"
        )

        kafkaTemplate.send(kafkaTopic, eksternFakturaStatusDto)

        await.timeout(20, TimeUnit.SECONDS)
            .until {
                fakturaRepository.findById(faktura.id!!)?.eksternFakturaStatus?.isNotEmpty()
            }

        val eksternFakturaStatus = fakturaRepository.findById(faktura.id!!)?.eksternFakturaStatus?.sortedBy { it.dato }?.get(0)

        eksternFakturaStatus.shouldNotBeNull()
        eksternFakturaStatus.status.shouldBe(FakturaStatus.FEIL)
        eksternFakturaStatus.feilMelding.shouldBe("Feil med faktura, mangler faktura referanse nummer")
    }


    @Test
    fun `les faktura fra kafka kø og lagre melding fra OEBS i DB, sjekk manglende betaling`(){
        val faktura = lagFakturaMedSerie(
            Faktura()
        )
        val eksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = faktura.id.toString(),
            fakturaNummer = "82",
            dato = LocalDate.now(),
            status = FakturaStatus.MANGLENDE_INNBETALING,
            fakturaBelop = BigDecimal(4000.00),
            ubetaltBelop = BigDecimal(2000.00),
            feilmelding = null
        )

        kafkaTemplate.send(kafkaTopic, eksternFakturaStatusDto)

        await.timeout(20, TimeUnit.SECONDS)
            .until {
                fakturaRepository.findById(faktura.id!!)?.eksternFakturaStatus?.isNotEmpty()
            }

        val eksternFakturaStatus = fakturaRepository.findById(faktura.id!!)?.eksternFakturaStatus?.sortedBy { it.dato }?.get(0)

        eksternFakturaStatus.shouldNotBeNull()
        eksternFakturaStatus.status.shouldBe(FakturaStatus.MANGLENDE_INNBETALING)
        eksternFakturaStatus.ubetaltBelop!!.shouldBeLessThan(eksternFakturaStatus.fakturaBelop!!)
    }

    @Disabled
    @Test
    fun `les faktura fra kakfak kø og sjekk at status blir satt til BETALT`() {
        val faktura = lagFakturaMedSerie(
            Faktura(
                status = FakturaStatus.BESTILLT,
            )
        )
        val eksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = faktura.id.toString(),
            fakturaNummer = "82",
            dato = LocalDate.now(),
            status = FakturaStatus.INNE_I_OEBS,
            fakturaBelop = BigDecimal(1000.00),
            ubetaltBelop = BigDecimal(2000.00),
            feilmelding = null
        )


        kafkaTemplate.send(kafkaTopic, eksternFakturaStatusDto)


        await.timeout(20, TimeUnit.SECONDS)
            .until {
                fakturaRepository.findById(faktura.id!!)!!.status == FakturaStatus.BETALT
            }

    }
}
