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
    @Autowired private val fakturaserieRepository: FakturaserieRepository
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
        val fakturaId = 1L
        val faktura = lagFaktura(fakturaId).apply {
            fakturaserie = lagFakturaserie()
        }
        val fakturaserie = lagFakturaserie().apply { this.faktura = listOf(faktura) }

        fakturaserieRepository.save(
            fakturaserie
        ).apply {
            addCleanUpAction { fakturaserieRepository.delete(this) }
        }

        eksternFakturaStatusService.lagreEksternFakturaStatusMelding(lagEksternFakturaStatusDto(faktura.id.toString()))

        TestQueue.manglendeFakturabetalingMeldinger.shouldHaveSize(1)
        val eksternFakturaStatus = eksternFakturaStatusRepository.findById(fakturaId)!!

        eksternFakturaStatus.sendt.shouldBe(true)
    }

    fun lagFaktura(id: Long? = 1): Faktura {
        return Faktura(
            id,
            LocalDate.of(2022, 5, 1),
            LocalDate.of(2022, 5, 1),
            FakturaStatus.OPPRETTET,
            fakturaLinje = listOf(
                FakturaLinje(
                    100,
                    null,
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(2023, 5, 1),
                    beskrivelse = "En beskrivelse",
                    belop = BigDecimal(90000),
                    antall = BigDecimal(1),
                    enhetsprisPerManed = BigDecimal(18000)
                ),
            )
        )
    }

    fun lagFakturaserie(): Fakturaserie {
        return Fakturaserie( referanse = "en_referanse",
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
            referanseBruker = "Referanse bruker",
            referanseNAV = "Referanse NAV",
            startdato = LocalDate.of(2022, 1, 1),
            sluttdato = LocalDate.of(2023, 5, 1),
            status = FakturaserieStatus.OPPRETTET,
            intervall = FakturaserieIntervall.KVARTAL,
            faktura = listOf(),
            fullmektig = Fullmektig(
                fodselsnummer = "12129012345",
                kontaktperson = "Test",
                organisasjonsnummer = ""
            ),
            fodselsnummer = "12345678911"
        )
    }
}
