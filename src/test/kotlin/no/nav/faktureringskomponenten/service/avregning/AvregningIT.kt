package no.nav.faktureringskomponenten.service.avregning

import com.nimbusds.jose.JOSEObjectType
import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import mu.KotlinLogging
import no.nav.faktureringskomponenten.PostgresTestContainerBase
import no.nav.faktureringskomponenten.controller.FakturaserieRepositoryForTesting
import no.nav.faktureringskomponenten.controller.dto.FakturaserieRequestDto
import no.nav.faktureringskomponenten.controller.dto.NyFakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.dto.forTest
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
    @Autowired private val unleash: FakeUnleash
) : PostgresTestContainerBase() {
    @AfterEach
    fun cleanUp() {
        addCleanUpAction {
            fakturaserieRepository.deleteAll()
        }
        unmockkStatic(LocalDate::class)
        unleash.disableAll()
    }

    @Test
    fun `erstatt fakturaserie med endringer tilbake i tid, sjekk avregning`() {
        val begynnelseAvDesember2023 = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns begynnelseAvDesember2023
        // Opprinnelig serie
        val opprinneligFakturaserieDto = FakturaserieRequestDto.forTest {
            periode {
                månedspris = 1000
                fra = "2024-01-01"
                til = "2024-12-31"
                beskrivelse = "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
            }
            periode {
                månedspris = 2000
                fra = "2024-01-01"
                til = "2024-12-31"
                beskrivelse = "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"
            }
        }

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
        val fakturaserieDto2 = FakturaserieRequestDto.forTest {
            fakturaserieReferanse = opprinneligFakturaserieReferanse
            periode {
                månedspris = 1000
                fra = "2024-01-01"
                til = "2024-12-31"
                beskrivelse = "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
            }
            periode {
                månedspris = 2000
                fra = "2024-01-01"
                til = "2024-02-29"
                beskrivelse = "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"
            }
            periode {
                månedspris = 3000
                fra = "2024-03-01"
                til = "2024-12-31"
                beskrivelse = "Inntekt: 30000, Dekning: Pensjon og helsedel, Sats 10%"
            }
        }

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
                            FakturaLinje.forTest {
                                fra = "2024-01-01"
                                til = "2024-03-31"
                                beskrivelse = "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 10000,00 - tidligere beløp: 9000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 1000
                                avregningNyttBeloep = BigDecimal("10000.00")
                                avregningForrigeBeloep = BigDecimal("9000.00")
                                belop = BigDecimal("1000.00")
                            }
                        )
                }
                get(1).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2024-04-01"
                                til = "2024-06-30"
                                beskrivelse = "Periode: 01.04.2024 - 30.06.2024\nNytt beløp: 12000,00 - tidligere beløp: 9000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 3000
                                avregningNyttBeloep = BigDecimal("12000.00")
                                avregningForrigeBeloep = BigDecimal("9000.00")
                                belop = BigDecimal("3000.00")
                            }
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
        val fakturaserieDto3 = FakturaserieRequestDto.forTest {
            fakturaserieReferanse = serieRef2
            periode {
                månedspris = 1000
                fra = "2024-01-01"
                til = "2024-12-31"
                beskrivelse = "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
            }
            periode {
                månedspris = 2000
                fra = "2024-01-01"
                til = "2024-01-31"
                beskrivelse = "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"
            }
            periode {
                månedspris = 3000
                fra = "2024-02-01"
                til = "2024-12-31"
                beskrivelse = "Inntekt: 30000, Dekning: Pensjon og helsedel, Sats 10%"
            }
        }

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
                            FakturaLinje.forTest {
                                fra = "2024-01-01"
                                til = "2024-03-31"
                                beskrivelse = "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 11000,00 - tidligere beløp: 10000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 1000
                                avregningNyttBeloep = BigDecimal("11000.00")
                                avregningForrigeBeloep = BigDecimal("10000.00")
                                belop = BigDecimal("1000.00")
                            }
                    }
                }
                get(1).run {
                    status.shouldBe(FakturaStatus.BESTILT)
                    fakturaLinje.shouldHaveSize(1).run {
                        single() shouldBe
                            FakturaLinje.forTest {
                                fra = "2024-04-01"
                                til = "2024-06-30"
                                beskrivelse = "Periode: 01.04.2024 - 30.06.2024\nNytt beløp: 12000,00 - tidligere beløp: 12000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 0
                                avregningNyttBeloep = BigDecimal("12000.00")
                                avregningForrigeBeloep = BigDecimal("12000.00")
                                belop = BigDecimal("0.00")
                            }
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

        totalBelop.shouldBe(orginalTotal.add(avregning1Total.add(avregning2Total)))
    }

    @Test
    fun `erstatt fakturaserie med endringer tilbake i tid - avregner kun mot faktura i dette kalenderåret`() {
        unleash.enableAll()
        val begynnelseAvDesember2022 = LocalDate.of(2022, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns begynnelseAvDesember2022
        // Opprinnelig serie
        val opprinneligFakturaserieDto = FakturaserieRequestDto.forTest {
            periode {
                månedspris = 1000
                fra = "2022-01-01"
                til = "2023-12-31"
                beskrivelse = "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
            }
            periode {
                månedspris = 2000
                fra = "2022-01-01"
                til = "2023-12-31"
                beskrivelse = "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"
            }
        }

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
                .returnResult().responseBody!!.fakturaserieReferanse

        val opprinneligeFakturaer = fakturaRepository.findByFakturaserieReferanse(opprinneligFakturaserieReferanse)
            .sortedBy(Faktura::getPeriodeFra)
        opprinneligeFakturaer.shouldHaveSize(5)
        opprinneligeFakturaer.forEach {
            it.status = FakturaStatus.BESTILT
            fakturaRepository.save(it)

        }

        fakturaserieRepository.findByReferanse(opprinneligFakturaserieReferanse).shouldNotBeNull().run {
            status = FakturaserieStatus.UNDER_BESTILLING
            fakturaserieRepository.save(this)
        }

        // Serie 2 med avregning
        val fakturaserieDto2 = FakturaserieRequestDto.forTest {
            fakturaserieReferanse = opprinneligFakturaserieReferanse
            periode {
                månedspris = 1000
                fra = "2023-01-01"
                til = "2023-12-31"
                beskrivelse = "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
            }
            periode {
                månedspris = 2000
                fra = "2023-01-01"
                til = "2023-02-28"
                beskrivelse = "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"
            }
            periode {
                månedspris = 3000
                fra = "2023-03-01"
                til = "2023-12-31"
                beskrivelse = "Inntekt: 30000, Dekning: Pensjon og helsedel, Sats 10%"
            }
        }

        // Setter now frem et år slik at 2022 fakturaer ikke skal brukes i avregning
        val begynnelseAvDesember2023 = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns begynnelseAvDesember2023

        val serieRef2 =
            postLagNyFakturaserieRequest(fakturaserieDto2).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
                .returnResult().responseBody!!.fakturaserieReferanse

        val fakturaer2 = fakturaRepository.findByFakturaserieReferanse(serieRef2)
        val avregningsfakturaer2 = fakturaer2.filter { it.erAvregningsfaktura() }


        avregningsfakturaer2.shouldHaveSize(4).sortedBy { it.getPeriodeFra() }
            .run {
                get(0).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2023-01-01"
                                til = "2023-03-31"
                                beskrivelse = "Periode: 01.01.2023 - 31.03.2023\nNytt beløp: 10000,00 - tidligere beløp: 9000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 1000
                                avregningNyttBeloep = BigDecimal("10000.00")
                                avregningForrigeBeloep = BigDecimal("9000.00")
                                belop = BigDecimal("1000.00")
                            }
                        )
                }
                get(1).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2023-04-01"
                                til = "2023-06-30"
                                beskrivelse = "Periode: 01.04.2023 - 30.06.2023\nNytt beløp: 12000,00 - tidligere beløp: 9000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 3000
                                avregningNyttBeloep = BigDecimal("12000.00")
                                avregningForrigeBeloep = BigDecimal("9000.00")
                                belop = BigDecimal("3000.00")
                            }
                        )
                }
                get(2).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2023-07-01"
                                til = "2023-09-30"
                                beskrivelse = "Periode: 01.07.2023 - 30.09.2023\nNytt beløp: 12000,00 - tidligere beløp: 9000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 3000
                                avregningNyttBeloep = BigDecimal("12000.00")
                                avregningForrigeBeloep = BigDecimal("9000.00")
                                belop = BigDecimal("3000.00")
                            }
                        )
                }
                get(3).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2023-10-01"
                                til = "2023-12-31"
                                beskrivelse = "Periode: 01.10.2023 - 31.12.2023\nNytt beløp: 12000,00 - tidligere beløp: 9000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 3000
                                avregningNyttBeloep = BigDecimal("12000.00")
                                avregningForrigeBeloep = BigDecimal("9000.00")
                                belop = BigDecimal("3000.00")
                            }
                        )
                }
            }


    }

    @Test
    fun `avregning 2 ganger kun frem i tid - verifisering av prodfeilfix`() {
        unleash.disableAll()
        val januar2026 = LocalDate.of(2026, 1, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns januar2026

        // Opprinnelig serie
        val opprinneligFakturaserieDto = FakturaserieRequestDto.forTest {
            periode {
                månedspris = 1000
                fra = "2026-01-01"
                til = "2026-12-31"
                beskrivelse = "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
            }
            periode {
                månedspris = 1000
                fra = "2027-01-01"
                til = "2027-01-31"
                beskrivelse = "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
            }
        }

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
                .returnResult().responseBody!!.fakturaserieReferanse

        val opprinneligeFakturaer = fakturaRepository.findByFakturaserieReferanse(opprinneligFakturaserieReferanse)
            .sortedBy(Faktura::getPeriodeFra)
        opprinneligeFakturaer.shouldHaveSize(5).sortedBy { it.getPeriodeFra() }
            .run {
                get(0).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-01-01"
                                til = "2026-03-31"
                                beskrivelse = "Periode: 01.01.2026 - 31.03.2026\n" +
                                    "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
                                antall = BigDecimal("3.00")
                                månedspris = 1000
                                belop = BigDecimal("3000.00")
                            }
                        )
                }
                get(1).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-04-01"
                                til = "2026-06-30"
                                beskrivelse = "Periode: 01.04.2026 - 30.06.2026\n" +
                                    "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
                                antall = BigDecimal("3.00")
                                månedspris = 1000
                                belop = BigDecimal("3000.00")
                            }
                        )
                }
                get(2).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-07-01"
                                til = "2026-09-30"
                                beskrivelse = "Periode: 01.07.2026 - 30.09.2026\n" +
                                    "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
                                antall = BigDecimal("3.00")
                                månedspris = 1000
                                belop = BigDecimal("3000.00")
                            }
                        )
                }
                get(3).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-10-01"
                                til = "2026-12-31"
                                beskrivelse = "Periode: 01.10.2026 - 31.12.2026\n" +
                                    "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
                                antall = BigDecimal("3.00")
                                månedspris = 1000
                                belop = BigDecimal("3000.00")
                            }
                        )
                }
                get(4).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2027-01-01"
                                til = "2027-01-31"
                                beskrivelse = "Periode: 01.01.2027 - 31.01.2027\n" +
                                    "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
                                antall = BigDecimal("1.00")
                                månedspris = 1000
                                belop = BigDecimal("1000.00")
                            }
                        )
                }
            }

        opprinneligeFakturaer.forEach {
            it.status = FakturaStatus.BESTILT
            fakturaRepository.save(it)

        }

        fakturaserieRepository.findByReferanse(opprinneligFakturaserieReferanse).shouldNotBeNull().run {
            status = FakturaserieStatus.UNDER_BESTILLING
            fakturaserieRepository.save(this)
        }

        // Serie 2 med avregning
        val fakturaserieDto2 = FakturaserieRequestDto.forTest {
            fakturaserieReferanse = opprinneligFakturaserieReferanse
            periode {
                månedspris = 1000
                fra = "2026-01-01"
                til = "2026-12-31"
                beskrivelse = "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
            }
            periode {
                månedspris = 1000
                fra = "2027-01-01"
                til = "2027-02-15"
                beskrivelse = "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"
            }
        }

        val serieRef2 =
            postLagNyFakturaserieRequest(fakturaserieDto2).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
                .returnResult().responseBody!!.fakturaserieReferanse

        val fakturaer2 = fakturaRepository.findByFakturaserieReferanse(serieRef2)
        fakturaer2.shouldHaveSize(6).sortedBy { it.getPeriodeFra() }
            .run {
                get(0).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-01-01"
                                til = "2026-03-31"
                                beskrivelse = "Periode: 01.01.2026 - 31.03.2026\nNytt beløp: 3000,00 - tidligere beløp: 3000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 0
                                avregningNyttBeloep = BigDecimal("3000.00")
                                avregningForrigeBeloep = BigDecimal("3000.00")
                                belop = BigDecimal.ZERO
                            }
                        )
                }
                get(1).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-04-01"
                                til = "2026-06-30"
                                beskrivelse = "Periode: 01.04.2026 - 30.06.2026\nNytt beløp: 3000,00 - tidligere beløp: 3000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 0
                                avregningNyttBeloep = BigDecimal("3000.00")
                                avregningForrigeBeloep = BigDecimal("3000.00")
                                belop = BigDecimal.ZERO
                            }
                        )
                }
                get(2).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-07-01"
                                til = "2026-09-30"
                                beskrivelse = "Periode: 01.07.2026 - 30.09.2026\nNytt beløp: 3000,00 - tidligere beløp: 3000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 0
                                avregningNyttBeloep = BigDecimal("3000.00")
                                avregningForrigeBeloep = BigDecimal("3000.00")
                                belop = BigDecimal.ZERO
                            }
                        )
                }
                get(3).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-10-01"
                                til = "2026-12-31"
                                beskrivelse = "Periode: 01.10.2026 - 31.12.2026\nNytt beløp: 3000,00 - tidligere beløp: 3000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 0
                                avregningNyttBeloep = BigDecimal("3000.00")
                                avregningForrigeBeloep = BigDecimal("3000.00")
                                belop = BigDecimal.ZERO
                            }
                        )
                }
                get(4).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2027-01-01"
                                til = "2027-01-31"
                                beskrivelse = "Periode: 01.01.2027 - 31.01.2027\nNytt beløp: 1000,00 - tidligere beløp: 1000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 0
                                avregningNyttBeloep = BigDecimal("1000.00")
                                avregningForrigeBeloep = BigDecimal("1000.00")
                                belop = BigDecimal.ZERO
                            }
                        )
                }
                get(5).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2027-02-01"
                                til = "2027-02-15"
                                beskrivelse = "Periode: 01.02.2027 - 15.02.2027\n" +
                                    "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"
                                antall = BigDecimal("0.54")
                                månedspris = 1000
                                belop = BigDecimal("540.00")
                            }
                        )
                }
            }

        fakturaer2.forEach {
            it.status = FakturaStatus.BESTILT
            fakturaRepository.save(it)
        }

        val fakturaserieDto3 = FakturaserieRequestDto.forTest {
            fakturaserieReferanse = serieRef2
            periode {
                månedspris = 1500
                fra = "2026-01-01"
                til = "2026-12-31"
                beskrivelse = "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
            }
            periode {
                månedspris = 1500
                fra = "2027-01-01"
                til = "2027-02-15"
                beskrivelse = "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"
            }
        }

        val serieRef3 =
            postLagNyFakturaserieRequest(fakturaserieDto3).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
                .returnResult().responseBody!!.fakturaserieReferanse

        val fakturaer3 = fakturaRepository.findByFakturaserieReferanse(serieRef3)
        fakturaer3.shouldHaveSize(6).sortedBy { it.getPeriodeFra() }
            .run {
                get(0).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-01-01"
                                til = "2026-03-31"
                                beskrivelse = "Periode: 01.01.2026 - 31.03.2026\nNytt beløp: 4500,00 - tidligere beløp: 3000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 1500
                                avregningNyttBeloep = BigDecimal("4500.00")
                                avregningForrigeBeloep = BigDecimal("3000.00")
                                belop = BigDecimal("1500.00")
                            }
                        )
                }
                get(1).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-04-01"
                                til = "2026-06-30"
                                beskrivelse = "Periode: 01.04.2026 - 30.06.2026\nNytt beløp: 4500,00 - tidligere beløp: 3000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 1500
                                avregningNyttBeloep = BigDecimal("4500.00")
                                avregningForrigeBeloep = BigDecimal("3000.00")
                                belop = BigDecimal("1500.00")
                            }
                        )
                }
                get(2).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-07-01"
                                til = "2026-09-30"
                                beskrivelse = "Periode: 01.07.2026 - 30.09.2026\nNytt beløp: 4500,00 - tidligere beløp: 3000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 1500
                                avregningNyttBeloep = BigDecimal("4500.00")
                                avregningForrigeBeloep = BigDecimal("3000.00")
                                belop = BigDecimal("1500.00")
                            }
                        )
                }
                get(3).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2026-10-01"
                                til = "2026-12-31"
                                beskrivelse = "Periode: 01.10.2026 - 31.12.2026\nNytt beløp: 4500,00 - tidligere beløp: 3000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 1500
                                avregningNyttBeloep = BigDecimal("4500.00")
                                avregningForrigeBeloep = BigDecimal("3000.00")
                                belop = BigDecimal("1500.00")
                            }
                        )
                }
                get(4).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2027-01-01"
                                til = "2027-01-31"
                                beskrivelse = "Periode: 01.01.2027 - 31.01.2027\nNytt beløp: 1500,00 - tidligere beløp: 1000,00"
                                antall = BigDecimal("1.00")
                                månedspris = 500
                                avregningNyttBeloep = BigDecimal("1500.00")
                                avregningForrigeBeloep = BigDecimal("1000.00")
                                belop = BigDecimal("500.00")
                            }
                        )
                }
                get(5).run {
                    fakturaLinje.single()
                        .shouldBe(
                            FakturaLinje.forTest {
                                fra = "2027-02-01"
                                til = "2027-02-15"
                                beskrivelse = "Periode: 01.02.2027 - 15.02.2027\nNytt beløp: 810,00 - tidligere beløp: 540,00"
                                antall = BigDecimal("1.00")
                                månedspris = 270
                                avregningNyttBeloep = BigDecimal("810.00")
                                avregningForrigeBeloep = BigDecimal("540.00")
                                belop = BigDecimal("270.00")
                            }
                        )
                }
            }
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
