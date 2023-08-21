package no.nav.faktureringskomponenten.service.integration.kafka

import io.kotest.matchers.bigdecimal.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaMottattStatus
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottattRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
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
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@ActiveProfiles("itest", "embeded-kafka")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
class FakturaMottattConsumerIT(
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired private val fakturaMottattRepository: FakturaMottattRepository,
    @Autowired @Qualifier("fakturaMottatt") private var kafkaTemplate: KafkaTemplate<String, FakturaMottattDto>
) : EmbeddedKafkaBase(fakturaserieRepository) {

    @Test
    fun `les faktura fra kafka kø og lagre melding fra OEBS i DB`(){
        val faktura = lagFakturaMedSerie(
            Faktura()
        )
        val fakturaMottattDto = FakturaMottattDto(
            fakturaReferanseNr = faktura.id.toString(),
            fakturaNummer = "82",
            dato = LocalDate.now(),
            status = FakturaMottattStatus.INNE_I_OEBS,
            fakturaBelop = BigDecimal(4000),
            ubetaltBelop = BigDecimal(2000),
            feilmelding = null
        )

        kafkaTemplate.send(kafkaTopic, fakturaMottattDto)

        await.timeout(20, TimeUnit.SECONDS)
            .until {
                fakturaMottattRepository.findAllByFakturaReferanseNr(fakturaMottattDto.fakturaReferanseNr)?.isNotEmpty()
            }
    }

    @Test
    fun `les faktura fra kafka kø og lagre melding fra OEBS i DB, sjekk feilmelding finnes`(){
        val faktura = lagFakturaMedSerie(
            Faktura()
        )
        val fakturaMottattDto = FakturaMottattDto(
            fakturaReferanseNr = faktura.id.toString(),
            fakturaNummer = "82",
            dato = LocalDate.now(),
            status = FakturaMottattStatus.FEIL,
            fakturaBelop = BigDecimal(4000),
            ubetaltBelop = BigDecimal(2000),
            feilmelding = "Feil med faktura, mangler faktura referanse nummer"
        )

        kafkaTemplate.send(kafkaTopic, fakturaMottattDto)

        await.timeout(20, TimeUnit.SECONDS)
            .until {
                fakturaMottattRepository.findAllByFakturaReferanseNr(fakturaMottattDto.fakturaReferanseNr)?.isNotEmpty()
            }

        val fakturaMottatt = fakturaMottattRepository.findAllByFakturaReferanseNr(fakturaMottattDto.fakturaReferanseNr)?.sortedBy { it.dato }?.get(0)

        fakturaMottatt.shouldNotBeNull()
        fakturaMottatt.status.shouldBe(FakturaMottattStatus.FEIL)
        fakturaMottatt.feilMelding.shouldBe("Feil med faktura, mangler faktura referanse nummer")
    }


    @Test
    fun `les faktura fra kafka kø og lagre melding fra OEBS i DB, sjekk manglende betaling`(){
        val faktura = lagFakturaMedSerie(
            Faktura()
        )
        val fakturaMottattDto = FakturaMottattDto(
            fakturaReferanseNr = faktura.id.toString(),
            fakturaNummer = "82",
            dato = LocalDate.now(),
            status = FakturaMottattStatus.MANGLENDE_BETALING,
            fakturaBelop = BigDecimal(4000.00),
            ubetaltBelop = BigDecimal(2000.00),
            feilmelding = null
        )

        kafkaTemplate.send(kafkaTopic, fakturaMottattDto)

        await.timeout(20, TimeUnit.SECONDS)
            .until {
                fakturaMottattRepository.findAllByFakturaReferanseNr(fakturaMottattDto.fakturaReferanseNr)?.isNotEmpty()
            }

        val fakturaMottatt = fakturaMottattRepository.findAllByFakturaReferanseNr(fakturaMottattDto.fakturaReferanseNr)?.sortedBy { it.dato }?.get(0)

        fakturaMottatt.shouldNotBeNull()
        fakturaMottatt.status.shouldBe(FakturaMottattStatus.MANGLENDE_BETALING)
        fakturaMottatt.ubetaltBelop!!.shouldBeLessThan(fakturaMottatt.fakturaBelop!!)
    }

    @Disabled
    @Test
    fun `les faktura fra kakfak kø og sjekk at status blir satt til BETALT`() {
        val faktura = lagFakturaMedSerie(
            Faktura(
                status = FakturaStatus.BESTILLT,
            )
        )
        val fakturaMottattDto = FakturaMottattDto(
            fakturaReferanseNr = faktura.id.toString(),
            fakturaNummer = "82",
            dato = LocalDate.now(),
            status = FakturaMottattStatus.INNE_I_OEBS,
            fakturaBelop = BigDecimal(1000.00),
            ubetaltBelop = BigDecimal(2000.00),
            feilmelding = null
        )


        kafkaTemplate.send(kafkaTopic, fakturaMottattDto)


        await.timeout(20, TimeUnit.SECONDS)
            .until {
                fakturaRepository.findById(faktura.id!!)!!.status == FakturaStatus.BETALT
            }

    }
}
