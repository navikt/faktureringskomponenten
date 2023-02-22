package no.nav.faktureringskomponenten.service.integration.kafka

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
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
class FakturaMottattConsumerIT(
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired @Qualifier("fakturaMottatt") private var kafkaTemplate: KafkaTemplate<String, FakturaMottattDto>
) : EmbeddedKafkaBase(fakturaserieRepository) {

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
}
