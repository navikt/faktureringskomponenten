package no.nav.faktureringskomponenten.itests

import com.nimbusds.jose.JOSEObjectType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.faktureringskomponenten.PostgresTestContainerBase
import no.nav.faktureringskomponenten.controller.FakturaserieRepositoryForTesting
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieRequestDto
import no.nav.faktureringskomponenten.controller.dto.NyFakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturaserieDto
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import no.nav.faktureringskomponenten.service.FakturaserieService
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaRepositoryForTesting
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.math.BigDecimal
import java.time.LocalDate

@ActiveProfiles("itest")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@EnableMockOAuth2Server
class FakturaKanselleringIT(
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired private val fakturaserieService: FakturaserieService,
    @Autowired private val fakturaserieRepositoryForTesting: FakturaserieRepositoryForTesting,
    @Autowired private val fakturaRepositoryForTesting: FakturaRepositoryForTesting,
    @Autowired private val server: MockOAuth2Server,
    @Autowired private val webClient: WebTestClient,
    @Autowired private val fakturaRepository: FakturaRepository,
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
        addCleanUpAction {
            fakturaserieRepository.deleteAll()
        }
        unmockkStatic(LocalDate::class)
    }

    @Test
    fun `Kansellerer fakturaserie - oppretter ny serie med kreditnota - disse sendes umiddelbart`() {
        val opprinneligFakturaserie = Fakturaserie.forTest {
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    månedspris = 10000
                }
            }
        }

        fakturaserieRepository.save(opprinneligFakturaserie)


        val krediteringsReferanse = fakturaserieService.kansellerFakturaserie(opprinneligFakturaserie.referanse)


        val krediteringsFakturaserie: Fakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(krediteringsReferanse)!!

        fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserie.referanse).apply {
            shouldNotBeNull()
            status.shouldBe(FakturaserieStatus.KANSELLERT)
            faktura.run {
                single()
                    .status.shouldBe(FakturaStatus.BESTILT)
            }
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

        val opprinneligFakturaserieTotalBelop = opprinneligFakturaserie.bestilteFakturaer()
            .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
            .map { it?.totalbeløp() }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        val krediteringFakturaserieTotalBelop = krediteringsFakturaserie.bestilteFakturaer()
            .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
            .map { it?.totalbeløp() }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        opprinneligFakturaserieTotalBelop.add(krediteringFakturaserieTotalBelop).shouldBe(BigDecimal.ZERO.setScale(2))
    }

    @Test
    fun `Avregning - så kansellering - totalen av foregående skal kanselleres`() {
        val begynnelseAvDesember2023 = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns begynnelseAvDesember2023
        // Opprinnelig serie
        val opprinneligFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(
                    1000,
                    "2024-01-01",
                    "2024-12-31",
                    "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
                ),
                FakturaseriePeriodeDto(
                    2000,
                    "2024-01-01",
                    "2024-12-31",
                    "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"
                ),
            )
        )

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
                .returnResult().responseBody!!.fakturaserieReferanse

        // "Bestiller" 2 fakturaer ved å sette status manuelt til BESTILT
        val opprinneligeFakturaer = fakturaRepository.findByFakturaserieReferanse(opprinneligFakturaserieReferanse)
            .sortedBy(Faktura::getPeriodeFra)
        opprinneligeFakturaer.shouldHaveSize(4)
        opprinneligeFakturaer[0].run {
            status = FakturaStatus.BESTILT
            eksternFakturaNummer = "8272123"
            fakturaRepository.save(this)
        }
        opprinneligeFakturaer[1].run {
            status = FakturaStatus.BESTILT
            eksternFakturaNummer = "8272124"
            fakturaRepository.save(this)
        }

        fakturaserieRepository.findByReferanse(opprinneligFakturaserieReferanse).shouldNotBeNull().run {
            status = FakturaserieStatus.UNDER_BESTILLING
            fakturaserieRepository.save(this)
        }

        // Serie 2 med avregning
        val fakturaserieDto2 = lagFakturaserieDto(
            referanseId = opprinneligFakturaserieReferanse,
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(
                    1000,
                    "2024-01-01",
                    "2024-12-31",
                    "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
                ),
                FakturaseriePeriodeDto(
                    2000,
                    "2024-01-01",
                    "2024-02-29",
                    "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"
                ),
                FakturaseriePeriodeDto(
                    3000,
                    "2024-03-01",
                    "2024-12-31",
                    "Inntekt: 30000, Dekning: Pensjon og helsedel, Sats 10%"
                ),
            )
        )

        val fakturaserieReferanse2 =
            postLagNyFakturaserieRequest(fakturaserieDto2).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
                .returnResult().responseBody!!.fakturaserieReferanse

        val fakturaer2 = fakturaRepository.findByFakturaserieReferanse(fakturaserieReferanse2)
        val avregningsfakturaer = fakturaer2.filter { it.erAvregningsfaktura() }


        avregningsfakturaer.shouldHaveSize(2).sortedBy { it.getPeriodeFra() }
            .run {
                get(0).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje(
                                periodeFra = LocalDate.of(2024, 1, 1),
                                periodeTil = LocalDate.of(2024, 3, 31),
                                beskrivelse = "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 10000,00 - tidligere beløp: 9000,00",
                                antall = BigDecimal("1.00"),
                                enhetsprisPerManed = BigDecimal("1000.00"),
                                avregningNyttBeloep = BigDecimal("10000.00"),
                                avregningForrigeBeloep = BigDecimal("9000.00"),
                                belop = BigDecimal("1000.00"),
                            )
                        )
                }
                get(1).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje(
                                periodeFra = LocalDate.of(2024, 4, 1),
                                periodeTil = LocalDate.of(2024, 6, 30),
                                beskrivelse = "Periode: 01.04.2024 - 30.06.2024\nNytt beløp: 12000,00 - tidligere beløp: 9000,00",
                                antall = BigDecimal("1.00"),
                                enhetsprisPerManed = BigDecimal("3000.00"),
                                avregningNyttBeloep = BigDecimal("12000.00"),
                                avregningForrigeBeloep = BigDecimal("9000.00"),
                                belop = BigDecimal("3000.00"),
                            )
                        )
                }
            }


        avregningsfakturaer.run {
            get(0).run {
                status = FakturaStatus.BESTILT
                eksternFakturaNummer = "1234"
                fakturaRepository.save(this)
            }
            get(1).run {
                status = FakturaStatus.BESTILT
                eksternFakturaNummer = "12345"
                fakturaRepository.save(this)
            }
        }

        fakturaserieRepository.findByReferanse(fakturaserieReferanse2).shouldNotBeNull().run {
            status = FakturaserieStatus.UNDER_BESTILLING
            fakturaserieRepository.save(this)
        }

        val verifiseringFakturaserie = fakturaserieService.lagNyFakturaserie(fakturaserieDto2.tilFakturaserieDto)
        val totalBelop = fakturaRepository.findByFakturaserieReferanse(verifiseringFakturaserie)
            .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
            .map { it?.totalbeløp() }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        val opprinneligTotal =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse)!!
                .bestilteFakturaer()
                .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
                .map { it?.totalbeløp() }
                .fold(BigDecimal.ZERO, BigDecimal::add)
        val avregning1Total = fakturaserieRepositoryForTesting.findByReferanseEagerly(fakturaserieReferanse2)!!.faktura
            .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
            .map { it?.totalbeløp() }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        totalBelop.shouldBe(opprinneligTotal.add(avregning1Total))

        val krediteringsReferanse = fakturaserieService.kansellerFakturaserie(fakturaserieReferanse2)


        val kanselleringTotalBelop =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(krediteringsReferanse)!!
                .faktura
                .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
                .map { it?.totalbeløp() }
                .fold(BigDecimal.ZERO, BigDecimal::add)

        val førstegangsbehandlingTotal =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse)!!
                .bestilteFakturaer()
                .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
                .map { it?.totalbeløp() }
                .fold(BigDecimal.ZERO, BigDecimal::add)
        val avregning1TotalBestilt =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(fakturaserieReferanse2)!!.bestilteFakturaer()
                .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
                .map { it?.totalbeløp() }
                .fold(BigDecimal.ZERO, BigDecimal::add)



        førstegangsbehandlingTotal.add(avregning1TotalBestilt).add(kanselleringTotalBelop)
            .shouldBe(BigDecimal.ZERO.setScale(2))
    }

    private fun postLagNyFakturaserieRequest(fakturaserieRequestDto: FakturaserieRequestDto): WebTestClient.ResponseSpec =
        webClient.post()
            .uri("/fakturaserier")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header("Nav-User-Id", "Z123456")
            .bodyValue(fakturaserieRequestDto)
            .headers {
                it.set(HttpHeaders.CONTENT_TYPE, "application/json")
                it.set(HttpHeaders.AUTHORIZATION, "Bearer " + token())
            }
            .exchange()

    private fun token(subject: String = "faktureringskomponenten-test"): String? =
        server.issueToken(
            "aad",
            "faktureringskomponenten-test",
            DefaultOAuth2TokenCallback(
                "aad",
                subject,
                JOSEObjectType.JWT.type,
                listOf("faktureringskomponenten-localhost"),
                mapOf("roles" to "faktureringskomponenten-skriv"),
                3600
            )
        ).serialize()

    fun lagFakturaserieDto(
        referanseId: String? = null,
        fakturaGjelderInnbetalingstype: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
        intervall: FakturaserieIntervall = FakturaserieIntervall.KVARTAL,
        fakturaseriePeriode: List<FakturaseriePeriodeDto>,
    ): FakturaserieRequestDto {
        return FakturaserieRequestDto(
            fodselsnummer = "12345678911",
            fakturaserieReferanse = referanseId,
            fullmektig = null,
            referanseBruker = "referanseBruker",
            referanseNAV = "referanseNav",
            fakturaGjelderInnbetalingstype = fakturaGjelderInnbetalingstype,
            intervall = intervall,
            perioder = fakturaseriePeriode
        )
    }
}