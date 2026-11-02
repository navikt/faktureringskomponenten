package no.nav.faktureringskomponenten.itests

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import jakarta.persistence.EntityManager
import no.nav.faktureringskomponenten.PostgresTestContainerBase
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.forTest
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.AdminService
import no.nav.faktureringskomponenten.service.BestillFakturaResultat
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import no.nav.faktureringskomponenten.service.cronjob.FakturaBestillCronjob
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import ulid.ULID
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ActiveProfiles("itest")
//@DataJpaTest(showSql = false) // Mangler bare å hindre at ting puttes i Transactional for at vi kan bruke dette
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
//@Import(FakturaBestillCronjob::class)
@EnableMockOAuth2Server
class FakturaBestillingServiceIT(
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired private val fakturaBestillCronjob: FakturaBestillCronjob,
    @Autowired private val fakturaBestillingService: FakturaBestillingService,
    @Autowired private val adminService: AdminService,
    @Autowired private val transactionManager: PlatformTransactionManager,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) : PostgresTestContainerBase() {

    private var fakturaReferanseNr: String = ""

    private object TestQueue {
        val fakturaBestiltMeldinger: MutableList<FakturaBestiltDto> = Collections.synchronizedList(mutableListOf())
        @Volatile var kastException: Boolean = false
        @Volatile var forsinkelseMs: Long = 0

        val fakturaBestiltProducer = FakturaBestiltProducer { fakturaBestiltDto ->
            if (kastException) throw IllegalStateException("Klarte ikke å legge melding på kø")
            Thread.sleep(forsinkelseMs)
            fakturaBestiltMeldinger.add(fakturaBestiltDto)
        }
    }

    @TestConfiguration
    class Config(
        @Autowired private val fakturaRepository: FakturaRepository,
        @Autowired private val fakturaserieRepository: FakturaserieRepository,
        @Autowired private val entityManager: EntityManager,
    ) {
        @Bean
        @Primary
        fun testFakturaService(): FakturaBestillingService {
            return FakturaBestillingService(
                fakturaRepository, fakturaserieRepository, TestQueue.fakturaBestiltProducer, entityManager
            )
        }
    }

    @AfterEach
    fun cleanup() {
        TestQueue.fakturaBestiltMeldinger.clear()
        TestQueue.kastException = false
        TestQueue.forsinkelseMs = 0
    }

    @BeforeEach
    fun before() {
        fakturaReferanseNr = lagFakturaSerie()
    }

    private fun lagFakturaSerie(): String =
        fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                fodselsnummer = "01234567890"
                faktura {
                    datoBestilt = LocalDate.now().plusDays(-1)
                    fakturaLinje {
                        beskrivelse = "test 1"
                        belop = BigDecimal(1000)
                        månedspris = 100
                    }
                }
            }
        ).faktura.first().apply {
            addCleanUpAction { fakturaserieRepository.delete(fakturaserie!!) }
        }.referanseNr


    @Test
    fun `test at melding blir sent på kø`() {
        fakturaBestillCronjob.bestillFaktura()

        TestQueue.fakturaBestiltMeldinger.shouldHaveSize(1)
        fakturaRepository.findByReferanseNr(fakturaReferanseNr)?.status
            .shouldBe(FakturaStatus.BESTILT)
    }

    @Test
    fun `beskrivelse lagres på faktura ved bestilling`() {
        fakturaBestillCronjob.bestillFaktura()

        val sendtMelding = TestQueue.fakturaBestiltMeldinger.single()
        val faktura = fakturaRepository.findByReferanseNr(fakturaReferanseNr)!!

        faktura.beskrivelse.shouldBe(sendtMelding.beskrivelse)
    }

    @Test
    fun `database oppdatering må rulles tilbake om det feiler når man sender melding på kø`() {
        TestQueue.kastException = true

        shouldThrow<IllegalStateException> {
            fakturaBestillCronjob.bestillFaktura()
        }.message.shouldBe("Klarte ikke å legge melding på kø")

        fakturaRepository.findByReferanseNr(fakturaReferanseNr)!!.status
            .shouldBe(FakturaStatus.OPPRETTET)
        TestQueue.fakturaBestiltMeldinger.shouldBeEmpty()
    }

    @Test
    fun `faktura som allerede er bestilt sendes ikke på nytt`() {
        fakturaBestillingService.bestillFaktura(fakturaReferanseNr) shouldBe true
        fakturaBestillingService.bestillFaktura(fakturaReferanseNr) shouldBe false

        TestQueue.fakturaBestiltMeldinger.shouldHaveSize(1)
    }

    @Test
    fun `bestillUmiddelbart setter dagens dato og sender én melding`() {
        adminService.bestillUmiddelbart(fakturaReferanseNr).shouldBeInstanceOf<BestillFakturaResultat.Bestilt>()

        TestQueue.fakturaBestiltMeldinger.single().fakturaReferanseNr shouldBe fakturaReferanseNr
        fakturaRepository.findByReferanseNr(fakturaReferanseNr)!!.run {
            status shouldBe FakturaStatus.BESTILT
            datoBestilt shouldBe LocalDate.now()
        }
    }

    @Test
    fun `bestillUmiddelbart ruller tilbake både dato og status når sending feiler`() {
        val opprinneligDato = fakturaRepository.findByReferanseNr(fakturaReferanseNr)!!.datoBestilt
        TestQueue.kastException = true

        shouldThrow<IllegalStateException> {
            adminService.bestillUmiddelbart(fakturaReferanseNr)
        }

        fakturaRepository.findByReferanseNr(fakturaReferanseNr)!!.run {
            status shouldBe FakturaStatus.OPPRETTET
            datoBestilt shouldBe opprinneligDato
        }
        TestQueue.fakturaBestiltMeldinger.shouldBeEmpty()
    }

    @Test
    fun `samtidige bestillinger av samme faktura sender bare én melding`() {
        // Holder transaksjonen åpen under sendingen, så kallene overlapper
        TestQueue.forsinkelseMs = 300
        val antallKall = 4
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(antallKall)

        val resultater = try {
            val futures = (1..antallKall).map {
                executor.submit<Boolean> {
                    start.await()
                    fakturaBestillingService.bestillFaktura(fakturaReferanseNr)
                }
            }
            start.countDown()
            futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        resultater.count { it } shouldBe 1
        TestQueue.fakturaBestiltMeldinger.shouldHaveSize(1)
        fakturaRepository.findByReferanseNr(fakturaReferanseNr)!!.status shouldBe FakturaStatus.BESTILT
    }

    @Test
    fun `bestiller ikke når fakturaen i persistenskonteksten har utdatert status`() {
        TransactionTemplate(transactionManager).executeWithoutResult { transaksjon ->
            // Lastes inn i persistenskonteksten, som med open-in-view i en webrequest
            fakturaRepository.findByReferanseNr(fakturaReferanseNr)!!.status shouldBe FakturaStatus.OPPRETTET
            jdbcTemplate.update("UPDATE faktura SET status = 'AVBRUTT' WHERE referanse_nr = ?", fakturaReferanseNr)

            fakturaBestillingService.bestillFaktura(fakturaReferanseNr) shouldBe false

            transaksjon.setRollbackOnly()
        }

        TestQueue.fakturaBestiltMeldinger.shouldBeEmpty()
    }

    @Test
    fun `bestiller ikke faktura med bestillingsdato etter angitt dato`() {
        val datoBestilt = fakturaRepository.findByReferanseNr(fakturaReferanseNr)!!.datoBestilt

        fakturaBestillingService.bestillFaktura(fakturaReferanseNr, datoBestilt.minusDays(1)) shouldBe false

        TestQueue.fakturaBestiltMeldinger.shouldBeEmpty()
        fakturaRepository.findByReferanseNr(fakturaReferanseNr)!!.status shouldBe FakturaStatus.OPPRETTET
    }

    @Test
    fun `sender ikke melding når skrivingen til databasen feiler`() {
        jdbcTemplate.execute(
            """
            CREATE FUNCTION feil_ved_bestilt() RETURNS trigger AS $$
            BEGIN
                IF NEW.status = 'BESTILT' THEN RAISE EXCEPTION 'Simulert databasefeil'; END IF;
                RETURN NEW;
            END $$ LANGUAGE plpgsql
            """
        )
        jdbcTemplate.execute(
            "CREATE TRIGGER feil_ved_bestilt BEFORE UPDATE ON faktura FOR EACH ROW EXECUTE FUNCTION feil_ved_bestilt()"
        )

        try {
            shouldThrow<Exception> { fakturaBestillingService.bestillFaktura(fakturaReferanseNr) }
        } finally {
            jdbcTemplate.execute("DROP TRIGGER feil_ved_bestilt ON faktura")
            jdbcTemplate.execute("DROP FUNCTION feil_ved_bestilt()")
        }

        TestQueue.fakturaBestiltMeldinger.shouldBeEmpty()
        fakturaRepository.findByReferanseNr(fakturaReferanseNr)!!.status shouldBe FakturaStatus.OPPRETTET
    }
}
