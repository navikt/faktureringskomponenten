package no.nav.faktureringskomponenten.itests

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.controller.FakturaserieRepositoryForTesting
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import no.nav.faktureringskomponenten.service.FakturaserieService
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.testutils.DomainTestFactory
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
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
//@DataJpaTest(showSql = false) // Mangler bare å hindre at ting puttes i Transactional for at vi kan bruke dette
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
//@Import(FakturaBestillCronjob::class)
@EnableMockOAuth2Server
class FakturaKanselleringIT(
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired private val fakturaserieService: FakturaserieService,
    @Autowired private val fakturaserieRepositoryForTesting: FakturaserieRepositoryForTesting
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
        fun testFakturaService(): FakturaBestillingService {
            return FakturaBestillingService(fakturaRepository, fakturaserieRepository, TestQueue.fakturaBestiltProducer)
        }
    }

    @AfterEach
    fun cleanup() {
        TestQueue.fakturaBestiltMeldinger.clear()
        TestQueue.kastException = false
    }

    @Test
    fun `Kansellerer fakturaserie - oppretter ny serie med kreditnota - disse sendes umiddelbart`() {
        val fakturaserie = DomainTestFactory.FakturaserieBuilder()
            .faktura(
                DomainTestFactory.FakturaBuilder()
                    .status(FakturaStatus.BESTILT)
                    .build()
            )
            .build()
        fakturaserieRepository.saveAndFlush(fakturaserie).apply {
            addCleanUpAction { fakturaserieRepository.delete(fakturaserie) }
        }


        val krediteringsReferanse = fakturaserieService.kansellerFakturaserie(fakturaserie.referanse)


        val krediteringsFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(krediteringsReferanse).apply {
                addCleanUpAction { fakturaserieRepository.delete(this!!) }
            }

        krediteringsFakturaserie.apply {
            shouldNotBeNull()
            status.shouldBe(FakturaserieStatus.FERDIG)
            faktura.run {
                single()
                    .status.shouldBe(FakturaStatus.BESTILT)
            }
        }

        TestQueue.fakturaBestiltMeldinger
            .single()
            .run {
                krediteringsReferanse.shouldBe(krediteringsReferanse)
                faktureringsDato.shouldBe(LocalDate.now())
                fakturaLinjer.single()
                    .belop.shouldBe(BigDecimal.valueOf(-10000).setScale(2))
            }

    }

}