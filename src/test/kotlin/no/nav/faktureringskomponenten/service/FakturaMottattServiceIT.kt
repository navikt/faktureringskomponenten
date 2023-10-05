package no.nav.faktureringskomponenten.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottattRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.ManglendeFakturabetalingProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.ManglendeFakturabetalingDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import no.nav.faktureringskomponenten.service.mappers.FakturaMottattMapper
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
class FakturaMottattServiceIT(
    @Autowired private val fakturaMottattMapper: FakturaMottattMapper,
    @Autowired private val fakturaMottattRepository: FakturaMottattRepository,
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository
) : PostgresTestContainerBase() {

    private lateinit var fakturaMottattService: FakturaMottattService

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
        fakturaMottattService = FakturaMottattService(fakturaRepository, fakturaMottattMapper, fakturaMottattRepository, TestQueue.manglendeFakturabetalingProducer)
    }

    @AfterEach
    fun cleanup() {
        TestQueue.manglendeFakturabetalingMeldinger.clear()
        TestQueue.kastException = false
    }

    fun lagFakturaMottattDto(fakturaReferanseNr: String) =
        FakturaMottattDto(
            fakturaReferanseNr = fakturaReferanseNr,
            fakturaNummer = "1",
            dato = LocalDate.now(),
            status = FakturaMottattStatus.MANGLENDE_INNBETALING,
            fakturaBelop = BigDecimal(1200.00),
            ubetaltBelop = BigDecimal(1000.00),
            feilmelding = null
        )

    @Test
    @Disabled("Disabler denne pga. visningsmøte. Legger inn denne igjen etterpå og refakturerer fakturaMottatt")
    fun `test at melding blir sendt til kø`() {
        val fakturaId = 1L
        val faktura = lagFaktura(fakturaId)

        fakturaserieRepository.save(
            Fakturaserie(
                faktura = listOf(
                    Faktura(datoBestilt = LocalDate.now().plusDays(100))
                )
            )
        ).apply { addCleanUpAction { fakturaserieRepository.delete(this) } }

        fakturaMottattService.lagreFakturaMottattMelding(lagFakturaMottattDto(faktura.id.toString()))

        TestQueue.manglendeFakturabetalingMeldinger.shouldHaveSize(1)
        val fakturaMottatt = fakturaMottattRepository.findById(fakturaId)!!

        fakturaMottatt.sendt.shouldBe(true)
    }

    fun lagFaktura(id: Long? = 1): Faktura {
        return Faktura(
            id,
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
        ).apply {
            fakturaserie =
                Fakturaserie(
                    100, referanse = "MEL-1",
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
}
