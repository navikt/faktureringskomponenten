package no.nav.faktureringskomponenten.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.cronjob.FakturaBestillCronjob
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDate

@ActiveProfiles("itest")
//@DataJpaTest(showSql = false) // Vil helst få dette til å funke med mindre @DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
//@Import(FakturaBestillCronjob::class)
@EnableMockOAuth2Server
class FakturaServiceIT(
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired private val fakturaBestillCronjob: FakturaBestillCronjob
) : PostgresTestContainerBase() {

    private object TestQueue {
        val fakturaBestiltMeldinger = mutableListOf<FakturaBestiltDto>()
        var kastException: Boolean = false

        val fakturaBestiltProducer = FakturaBestiltProducer { fakturaBestiltDto ->
            if (kastException) throw IllegalStateException("Klarte ikke å legge melding på kø")
            fakturaBestiltMeldinger.add(fakturaBestiltDto)
        }
    }

    @TestConfiguration
    class Config(
        @Autowired private val fakturaRepository: FakturaRepository,
        @Autowired private val fakturaserieRepository: FakturaserieRepository,
    ) {
        @Bean
        @Primary
        fun testFakturaService(): FakturaService {
            return FakturaService(fakturaRepository, fakturaserieRepository, TestQueue.fakturaBestiltProducer)
        }
    }

    @BeforeEach
    fun setup() {
        TestQueue.kastException = false
        fakturaserieRepository.saveAndFlush(
            Fakturaserie(
                vedtaksId = "MEL-1-9",
                fodselsnummer = "01234567890",
                faktura = mutableListOf(
                    Faktura(
                        datoBestilt = LocalDate.now().plusDays(-1),
                        fakturaLinje = mutableListOf(
                            FakturaLinje(
                                beskrivelse = "test 1",
                                belop = BigDecimal(1000),
                                enhetsprisPerManed = BigDecimal(100)
                            )
                        )
                    )
                )
            ).apply { faktura.forEach { it.fakturaserie = this } }
        )
    }

    @Test
    fun `test at melding blir sent på kø`() {
        fakturaBestillCronjob.bestillFaktura()

        TestQueue.fakturaBestiltMeldinger.shouldHaveSize(1)

        fakturaRepository.findById(1L)?.status
            .shouldBe(FakturaStatus.BESTILLT)
    }

    @Test
    fun `database oppdatering må rulles tilbake om det feiler når man sender melding på kø`() {
        TestQueue.kastException = true
        shouldThrow<IllegalStateException> {
            fakturaBestillCronjob.bestillFaktura()
        }.message.shouldBe("Klarte ikke å legge melding på kø")

        fakturaRepository.findById(1L)?.status
            .shouldBe(FakturaStatus.OPPRETTET)

        TestQueue.fakturaBestiltMeldinger.shouldBeEmpty()
    }
}