package no.nav.faktureringskomponenten.service.integration.kafka

import com.github.guepardoapps.kulid.ULID
import io.kotest.matchers.bigdecimal.shouldBeLessThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@ActiveProfiles("itest", "embeded-kafka")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableMockOAuth2Server
class EksternFakturaStatusConsumerIT(
    @Autowired private val fakturaRepository: FakturaRepositoryForTesting,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired @Qualifier("fakturaMottatt") private var kafkaTemplate: KafkaTemplate<String, EksternFakturaStatusDto>,
) : EmbeddedKafkaBase(fakturaserieRepository) {
    val fakturaReferanseNr = ULID.random()

    @Test
    fun `les faktura fra kafka kø og lagre melding fra OEBS i DB`(){
        val faktura = lagFakturaMedSerie(
            Faktura(referanseNr = fakturaReferanseNr)
        )

        val eksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = faktura.referanseNr,
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
                fakturaRepository.findByIdEagerly(faktura.id!!)?.eksternFakturaStatus?.isNotEmpty() ?: false
            }
    }

    @Test
    fun `les faktura fra kafka kø og lagre melding fra OEBS i DB, sjekk feilmelding finnes`(){
        val faktura = lagFakturaMedSerie(
            Faktura(referanseNr = fakturaReferanseNr)
        )

        val eksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = faktura.referanseNr,
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
                fakturaRepository.findByIdEagerly(faktura.id!!)?.eksternFakturaStatus?.isNotEmpty() ?: false
            }

        val eksternFakturaStatus = fakturaRepository.findByIdEagerly(faktura.id!!)?.eksternFakturaStatus?.sortedBy { it.dato }?.get(0)

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
            fakturaReferanseNr = faktura.referanseNr,
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
                fakturaRepository.findByIdEagerly(faktura.id!!)?.eksternFakturaStatus?.isNotEmpty() ?: false
            }

        val eksternFakturaStatus = fakturaRepository.findByIdEagerly(faktura.id!!)?.eksternFakturaStatus?.sortedBy { it.dato }?.get(0)

        eksternFakturaStatus.shouldNotBeNull()
        eksternFakturaStatus.status.shouldBe(FakturaStatus.MANGLENDE_INNBETALING)
        eksternFakturaStatus.ubetaltBelop!!.shouldBeLessThan(eksternFakturaStatus.fakturaBelop!!)
    }
}

interface FakturaRepositoryForTesting : JpaRepository<Faktura, String> {

    @Query("SELECT f FROM Faktura f JOIN fetch f.eksternFakturaStatus where f.id = :id")
    fun findByIdEagerly(id: Long): Faktura?
}
