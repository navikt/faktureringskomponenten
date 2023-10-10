package no.nav.faktureringskomponenten.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.EksternFakturaStatusRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.ManglendeFakturabetalingProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.ManglendeFakturabetalingDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import no.nav.faktureringskomponenten.service.mappers.EksternFakturaStatusMapper
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@ActiveProfiles("itest")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest
@EnableMockOAuth2Server
class EksternFakturaStatusServiceIT(
    @Autowired private val eksternFakturaStatusMapper: EksternFakturaStatusMapper,
    @Autowired private val eksternFakturaStatusRepository: EksternFakturaStatusRepository,
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired private val fakturaserieService: FakturaserieService
) : PostgresTestContainerBase() {

    private lateinit var eksternFakturaStatusService: EksternFakturaStatusService

    private object TestQueue {
        val manglendeFakturabetalingMeldinger = mutableListOf<ManglendeFakturabetalingDto>()
        var kastException: Boolean = false

        val manglendeFakturabetalingProducer = ManglendeFakturabetalingProducer { manglendeFakturabetaling ->
            if (kastException) throw IllegalStateException("Klarte ikke å legge melding på kø")
            manglendeFakturabetalingMeldinger.add(manglendeFakturabetaling)
        }
    }

    @BeforeEach
    fun before() {
        eksternFakturaStatusService = EksternFakturaStatusService(fakturaRepository, eksternFakturaStatusMapper, eksternFakturaStatusRepository, TestQueue.manglendeFakturabetalingProducer)


    }

    @AfterEach
    fun cleanup() {
        TestQueue.manglendeFakturabetalingMeldinger.clear()
        TestQueue.kastException = false
    }

    fun lagEksternFakturaStatusDto(fakturaReferanseNr: String) =
        EksternFakturaStatusDto(
            fakturaReferanseNr = fakturaReferanseNr,
            fakturaNummer = "1",
            dato = LocalDate.now(),
            status = FakturaStatus.MANGLENDE_INNBETALING,
            fakturaBelop = BigDecimal(1200.00),
            ubetaltBelop = BigDecimal(1000.00),
            feilmelding = null
        )

    @Test
    @Transactional
    fun `test at melding blir sendt til kø`() {
        val fakturaId = "1"
        lagFakturaSerie()

        eksternFakturaStatusService.lagreEksternFakturaStatusMelding(lagEksternFakturaStatusDto(fakturaId))

        TestQueue.manglendeFakturabetalingMeldinger.shouldHaveSize(1)
        val eksternFakturaStatus = eksternFakturaStatusRepository.findById(fakturaId.toLong())!!

        eksternFakturaStatus.sendt.shouldBe(true)
    }

    private fun lagFakturaSerie(): Long =
        fakturaserieRepository.saveAndFlush(
            Fakturaserie(
                referanse = "MEL-1-1",
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
        ).faktura.first().apply {
            addCleanUpAction { fakturaserieRepository.delete(fakturaserie!!) }
        }.id!!
}
