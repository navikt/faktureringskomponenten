package no.nav.faktureringskomponenten.service.avregning

import com.nimbusds.jose.JOSEObjectType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import mu.KotlinLogging
import no.nav.faktureringskomponenten.PostgresTestContainerBase
import no.nav.faktureringskomponenten.controller.FakturaserieRepositoryForTesting
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieRequestDto
import no.nav.faktureringskomponenten.controller.dto.NyFakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturaserieDto
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.FakturaserieService
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaRepositoryForTesting
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@ActiveProfiles("itest")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@EnableMockOAuth2Server
class AvregningIT(
    @Autowired private val server: MockOAuth2Server,
    @Autowired private val webClient: WebTestClient,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieService: FakturaserieService,
    @Autowired private val fakturaRepositoryForTesting: FakturaRepositoryForTesting,
    @Autowired private val fakturaserieRepositoryForTesting: FakturaserieRepositoryForTesting,
) : PostgresTestContainerBase() {
    @AfterEach
    fun cleanUp() {
        addCleanUpAction {
            fakturaserieRepository.deleteAll()
        }
        unmockkStatic(LocalDate::class)
    }

    @Test
    fun `erstatt fakturaserie med endringer tilbake i tid, sjekk avregning`() {
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

        val serieRef2 =
            postLagNyFakturaserieRequest(fakturaserieDto2).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
                .returnResult().responseBody!!.fakturaserieReferanse

        log.debug { "Tester 1. avregning" }
        val fakturaer2 = fakturaRepository.findByFakturaserieReferanse(serieRef2)
        val avregningsfakturaer2 = fakturaer2.filter { it.erAvregningsfaktura() }


        avregningsfakturaer2.shouldHaveSize(2).sortedBy { it.getPeriodeFra() }
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


        avregningsfakturaer2.run {
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

        fakturaserieRepository.findByReferanse(serieRef2).shouldNotBeNull().run {
            status = FakturaserieStatus.UNDER_BESTILLING
            fakturaserieRepository.save(this)
        }

        // Serie 3 med avregning
        val fakturaserieDto3 = lagFakturaserieDto(
            referanseId = serieRef2,
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
                    "2024-01-31",
                    "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"
                ),
                FakturaseriePeriodeDto(
                    3000,
                    "2024-02-01",
                    "2024-12-31",
                    "Inntekt: 30000, Dekning: Pensjon og helsedel, Sats 10%"
                ),
            )
        )

        val serieRef3 =
            postLagNyFakturaserieRequest(fakturaserieDto3).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
                .returnResult().responseBody!!.fakturaserieReferanse

        log.debug { "Tester 2. avregning" }
        fakturaRepository.findByFakturaserieReferanse(serieRef3)
            .shouldHaveSize(4)
            .filter { it.erAvregningsfaktura() }
            .run {
                get(0).run {
                    status.shouldBe(FakturaStatus.OPPRETTET)
                    fakturaLinje.shouldHaveSize(1).run {
                        single() shouldBe
                                FakturaLinje(
                                    periodeFra = LocalDate.of(2024, 1, 1),
                                    periodeTil = LocalDate.of(2024, 3, 31),
                                    beskrivelse = "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 11000,00 - tidligere beløp: 10000,00",
                                    antall = BigDecimal("1.00"),
                                    enhetsprisPerManed = BigDecimal("1000.00"),
                                    avregningNyttBeloep = BigDecimal("11000.00"),
                                    avregningForrigeBeloep = BigDecimal("10000.00"),
                                    belop = BigDecimal("1000.00"),
                                )
                    }
                }
                get(1).run {
                    status.shouldBe(FakturaStatus.BESTILT)
                    fakturaLinje.shouldHaveSize(1).run {
                        single() shouldBe
                                FakturaLinje(
                                    periodeFra = LocalDate.of(2024, 4, 1),
                                    periodeTil = LocalDate.of(2024, 6, 30),
                                    beskrivelse = "Periode: 01.04.2024 - 30.06.2024\nNytt beløp: 12000,00 - tidligere beløp: 12000,00",
                                    antall = BigDecimal("1.00"),
                                    enhetsprisPerManed = BigDecimal("0.00"),
                                    avregningNyttBeloep = BigDecimal("12000.00"),
                                    avregningForrigeBeloep = BigDecimal("12000.00"),
                                    belop = BigDecimal("0.00"),
                                )
                    }
                }
            }

        //lager en ny fakturaserie basert på siste fakturaserieDto, ser at summen av de tidligere seriene får lik total som denne

        val verifiseringFakturaserie = fakturaserieService.lagNyFakturaserie(fakturaserieDto3.tilFakturaserieDto)
        val totalBelop = fakturaRepository.findByFakturaserieReferanse(verifiseringFakturaserie)
            .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
            .map { it?.totalbeløp() }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        val orginalTotal = fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse)!!
            .bestilteFakturaer()
            .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
            .map { it?.totalbeløp() }
            .fold(BigDecimal.ZERO, BigDecimal::add)
        val avregning1Total = fakturaserieRepositoryForTesting.findByReferanseEagerly(serieRef2)!!.bestilteFakturaer()
                .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
                .map { it?.totalbeløp() }
                .fold(BigDecimal.ZERO, BigDecimal::add)
        val avregning2Total = fakturaserieRepositoryForTesting.findByReferanseEagerly(serieRef3)!!.faktura
            .map { fakturaRepositoryForTesting.findByfakturaAndLinjeEagerly(it.id) }
            .map { it?.totalbeløp() }
            .fold(BigDecimal.ZERO, BigDecimal::add)

        println(totalBelop)
        println(orginalTotal)
        println(avregning1Total)
        println(avregning2Total)

        totalBelop.shouldBe(orginalTotal.add(avregning1Total.add(avregning2Total)))
    }

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
}
