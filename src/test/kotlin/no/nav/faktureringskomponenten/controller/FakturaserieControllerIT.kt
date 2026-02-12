package no.nav.faktureringskomponenten.controller

import com.nimbusds.jose.JOSEObjectType
import io.kotest.inspectors.forAll
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.faktureringskomponenten.controller.FakturaserieController.KanselleringRequestDto
import no.nav.faktureringskomponenten.controller.dto.*
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.cronjob.FakturaBestillCronjob
import no.nav.faktureringskomponenten.service.integration.kafka.EmbeddedKafkaBase
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate

@Testcontainers
@ActiveProfiles("itest", "embeded-kafka")
@AutoConfigureWebTestClient
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
class FakturaserieControllerIT(
    @Autowired private val webClient: WebTestClient,
    @Autowired private val server: MockOAuth2Server,
    @Autowired private val fakturaserieRepositoryForTesting: FakturaserieRepositoryForTesting,
    @Autowired private val fakturaRepositoryForTesting: FakturaRepositoryForControllerTesting,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaBestillCronjob: FakturaBestillCronjob,
) : EmbeddedKafkaBase(fakturaserieRepository) {

    @AfterEach
    fun cleanUp() {
        unmockkStatic(LocalDate::class)
        addCleanUpAction {
            fakturaserieRepositoryForTesting.deleteAll()
        }
    }

    @Test
    fun `erstatt fakturaserie, erstatter opprinnelig og lager ny`() {
        val startDatoOpprinnelig = LocalDate.now().minusMonths(3)
        val sluttDatoOpprinnelig = LocalDate.now().plusMonths(9)
        val startDatoNy = LocalDate.now().minusMonths(2)
        val sluttDatoNy = LocalDate.now().plusMonths(8)

        val opprinneligFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    startDato = startDatoOpprinnelig
                    sluttDato = sluttDatoOpprinnelig
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk.expectBody(
                NyFakturaserieResponseDto::class.java
            ).returnResult().responseBody!!.fakturaserieReferanse

        val nyFakturaserieDto = lagFakturaserieDto(
            referanseId = opprinneligFakturaserieReferanse, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 18000
                    startDato = startDatoNy
                    sluttDato = sluttDatoNy
                    beskrivelse = "Inntekt fra Norge"
                },
                FakturaseriePeriodeDto.forTest {
                    månedspris = 24000
                    startDato = startDatoNy
                    sluttDato = sluttDatoNy
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val nyFakturaserieReferanse = postLagNyFakturaserieRequest(nyFakturaserieDto).expectStatus().isOk.expectBody(
            NyFakturaserieResponseDto::class.java
        ).returnResult().responseBody!!.fakturaserieReferanse


        val nyFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(nyFakturaserieReferanse).shouldNotBeNull()
        val oppdatertOpprinneligFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse)

        oppdatertOpprinneligFakturaserie.shouldNotBeNull().erstattetMed!!.id shouldBe nyFakturaserie.id
        oppdatertOpprinneligFakturaserie.status shouldBe FakturaserieStatus.ERSTATTET
        oppdatertOpprinneligFakturaserie.faktura.forEach {
            it.status.shouldBe(FakturaStatus.AVBRUTT)
        }

        nyFakturaserie.shouldNotBeNull().status shouldBe FakturaserieStatus.OPPRETTET
        nyFakturaserie.faktura.forEach {
            it.status shouldBe FakturaStatus.OPPRETTET
        }
    }

    /**
     * | Fakturaserie | 2024 q1 | 2024 q2        | Medlemskapsperiode  |
     * |--------------|---------|----------------|---------------------|
     * | s1           |         |   3000         | 01.04.24 - 30.06.24 |
     * | s2           |   2000  |      0         | 01.02.24 - 30.06.24 |
     *
     */
    @Test
    fun `erstatter opprinnelig fakturaserie med bestilt faktura for q2 med en ny fakturaserie med startdato i q1`() {
        mockkStatic(LocalDate::class)
        // setter en dato som gir mening i forhold til hva LocalDate.now() var når fakturaseriene ble laget
        every { LocalDate.now() } returns LocalDate.of(2024, 3, 19)

        val startDatoOpprinnelig = LocalDate.of(2024, 4, 1)
        val sluttDatoOpprinnelig = LocalDate.of(2024, 6, 30)

        val opprinneligFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 1000
                    startDato = startDatoOpprinnelig
                    sluttDato = sluttDatoOpprinnelig
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk.expectBody(
                NyFakturaserieResponseDto::class.java
            ).returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        // setter en dato som gir mening i forhold til hva LocalDate.now() var når ny vurdering fører til ny fakturaserie
        every { LocalDate.now() } returns LocalDate.of(2024, 6, 19)

        fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse)
            .shouldNotBeNull()
            .faktura.single().status.shouldBe(FakturaStatus.BESTILT)

        // Ny vurdering starter med periode 2024 q1 inkludert
        val startDatoNy = LocalDate.of(2024, 2, 1)
        val sluttDatoNy = LocalDate.of(2024, 6, 30)
        val nyFakturaserieDto = lagFakturaserieDto(
            referanseId = opprinneligFakturaserieReferanse, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 1000
                    startDato = startDatoNy
                    sluttDato = sluttDatoNy
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val nyFakturaserieReferanse = postLagNyFakturaserieRequest(nyFakturaserieDto).expectStatus().isOk.expectBody(
            NyFakturaserieResponseDto::class.java
        ).returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        val nyFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(nyFakturaserieReferanse).shouldNotBeNull()

        val opprinneligFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse).shouldNotBeNull()

        opprinneligFakturaserie.run {
            erstattetMed.shouldNotBeNull()
                .id shouldBe nyFakturaserie.id

            status shouldBe FakturaserieStatus.ERSTATTET

            faktura.shouldHaveSize(1)
                .single()
                .status shouldBe FakturaStatus.BESTILT
        }

        nyFakturaserie.run {
            status shouldBe FakturaserieStatus.UNDER_BESTILLING
            faktura.sortedByDescending { it.id }
                .shouldHaveSize(2)
                .map { it.id.shouldNotBeNull() }
                .map { fakturaRepositoryForTesting.findByIdEagerly(it).shouldNotBeNull() }
                .run {
                    first().run {
                        status shouldBe FakturaStatus.BESTILT
                        fakturaLinje.single().run {
                            periodeFra shouldBe LocalDate.of(2024, 2, 1)
                            belop.toString() shouldBe "2000.00"
                        }
                    }
                    last().run {
                        status shouldBe FakturaStatus.BESTILT
                        fakturaLinje.single().run {
                            periodeFra shouldBe LocalDate.of(2024, 4, 1)
                            belop.toString() shouldBe "0.00"
                        }
                    }
                }
        }
    }

    /**
     * | Fakturaserie | 2024 q2 | 2024 q2        | Medlemskapsperiode  |
     * |--------------|---------|----------------|---------------------|
     * | s1           |  1000   |   2000~        | 01.06.24 - 01.09.24 |
     * | s2           |  1000   |      0         | 01.05.24 - 01.09.24 |
     */
    @Test
    fun `erstatter opprinnelig fakturaserie med bestilt faktura med en ny fakturaserie med startdato 1 md tidligere`() {
        mockkStatic(LocalDate::class)
        // Setter en dato som skal gi umiddelbar fakturering
        every { LocalDate.now() } returns LocalDate.of(2025, 1, 23)

        val startDatoOpprinnelig = LocalDate.of(2024, 6, 1)
        val sluttDatoOpprinnelig = LocalDate.of(2024, 9, 1)

        val opprinneligFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 1000
                    startDato = startDatoOpprinnelig
                    sluttDato = sluttDatoOpprinnelig
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk.expectBody(
                NyFakturaserieResponseDto::class.java
            ).returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse)
            .shouldNotBeNull()
            .faktura.single().status.shouldBe(FakturaStatus.BESTILT)

        // Ny vurdering starter en måned tidligere
        val startDatoNy = LocalDate.of(2024, 5, 1)
        val sluttDatoNy = LocalDate.of(2024, 9, 1)
        val nyFakturaserieDto = lagFakturaserieDto(
            referanseId = opprinneligFakturaserieReferanse, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 1000
                    startDato = startDatoNy
                    sluttDato = sluttDatoNy
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val nyFakturaserieReferanse = postLagNyFakturaserieRequest(nyFakturaserieDto).expectStatus().isOk.expectBody(
            NyFakturaserieResponseDto::class.java
        ).returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        val nyFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(nyFakturaserieReferanse).shouldNotBeNull()

        val opprinneligFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse).shouldNotBeNull()

        opprinneligFakturaserie.run {
            erstattetMed.shouldNotBeNull()
                .id shouldBe nyFakturaserie.id

            status shouldBe FakturaserieStatus.ERSTATTET

            faktura.shouldHaveSize(1)
                .single()
                .status shouldBe FakturaStatus.BESTILT
        }

        nyFakturaserie.run {
            status shouldBe FakturaserieStatus.UNDER_BESTILLING
            faktura.sortedByDescending { it.id }
                .shouldHaveSize(2)
                .map { it.id.shouldNotBeNull() }
                .map { fakturaRepositoryForTesting.findByIdEagerly(it).shouldNotBeNull() }
                .run {
                    first().run {
                        status shouldBe FakturaStatus.BESTILT
                        fakturaLinje.single().run {
                            periodeFra shouldBe LocalDate.of(2024, 5, 1)
                            periodeTil shouldBe LocalDate.of(2024, 5, 31)
                            belop.toString() shouldBe "1000.00"
                        }
                    }
                    last().run {
                        status shouldBe FakturaStatus.BESTILT
                        fakturaLinje.single().run {
                            periodeFra shouldBe LocalDate.of(2024, 6, 1)
                            belop.toString() shouldBe "0.00"
                        }
                    }
                }
        }
    }

    /**
     * | Fakturaserie | 2024 q1 | 2024 q2        | Medlemskapsperiode  |
     * |--------------|---------|----------------|---------------------|
     * | s1           |  3000   |                | 01.01.24 - 30.03.31 |
     * | s2           | -3000   |   3000         | 01.04.24 - 30.06.24 |
     *
     */
    @Test
    fun `flytter medlemskapsperiode fra q1 til q2`() {
        mockkStatic(LocalDate::class)
        // setter en dato som gir mening i forhold til hva LocalDate.now() var når fakturaseriene ble laget
        every { LocalDate.now() } returns LocalDate.of(2024, 3, 19)

        val startDatoOpprinnelig = LocalDate.of(2024, 1, 1)
        val sluttDatoOpprinnelig = LocalDate.of(2024, 3, 31)

        val opprinneligFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 1000
                    startDato = startDatoOpprinnelig
                    sluttDato = sluttDatoOpprinnelig
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk.expectBody(
                NyFakturaserieResponseDto::class.java
            ).returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        // setter en dato som gir mening i forhold til hva LocalDate.now() var når ny vurdering fører til ny fakturaserie
        every { LocalDate.now() } returns LocalDate.of(2024, 6, 19)

        fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse)
            .shouldNotBeNull()
            .faktura.single().status.shouldBe(FakturaStatus.BESTILT)

        // Ny vurdering starter med periode 2024 q1 inkludert
        val startDatoNy = LocalDate.of(2024, 4, 1)
        val sluttDatoNy = LocalDate.of(2024, 6, 30)
        val nyFakturaserieDto = lagFakturaserieDto(
            referanseId = opprinneligFakturaserieReferanse, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 1000
                    startDato = startDatoNy
                    sluttDato = sluttDatoNy
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val nyFakturaserieReferanse = postLagNyFakturaserieRequest(nyFakturaserieDto).expectStatus().isOk.expectBody(
            NyFakturaserieResponseDto::class.java
        ).returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        val nyFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(nyFakturaserieReferanse).shouldNotBeNull()

        val opprinneligFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse).shouldNotBeNull()

        opprinneligFakturaserie.run {
            erstattetMed.shouldNotBeNull()
                .id shouldBe nyFakturaserie.id

            status shouldBe FakturaserieStatus.ERSTATTET

            faktura.shouldHaveSize(1)
                .single()
                .status shouldBe FakturaStatus.BESTILT
        }

        nyFakturaserie.run {
            status shouldBe FakturaserieStatus.UNDER_BESTILLING
            faktura.sortedByDescending { it.id }
                .shouldHaveSize(2)
                .map { it.id.shouldNotBeNull() }
                .map { fakturaRepositoryForTesting.findByIdEagerly(it).shouldNotBeNull() }
                .run {
                    first().run {
                        status shouldBe FakturaStatus.BESTILT
                        fakturaLinje.single().run {
                            periodeFra shouldBe LocalDate.of(2024, 4, 1)
                            belop.toString() shouldBe "3000.00"
                        }
                    }
                    last().run {
                        status shouldBe FakturaStatus.BESTILT
                        fakturaLinje.single().run {
                            periodeFra shouldBe LocalDate.of(2024, 1, 1)
                            belop.toString() shouldBe "-3000.00"
                        }
                    }
                }
        }
    }

    @Test
    fun `erstatt fakturaserie, første faktura er Bestilt, erstatter opprinnelig og lager ny`() {
        val begynnelseAvDesember = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns begynnelseAvDesember

        val startDatoOpprinnelig = LocalDate.now().minusMonths(3)
        val sluttDatoOpprinnelig = LocalDate.now().plusMonths(9)

        val opprinneligFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    startDato = startDatoOpprinnelig
                    sluttDato = sluttDatoOpprinnelig
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk.expectBody(
                NyFakturaserieResponseDto::class.java
            ).returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        val nyFakturaserieDto = lagFakturaserieDto(
            referanseId = opprinneligFakturaserieReferanse, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 20000
                    startDato = startDatoOpprinnelig
                    sluttDato = sluttDatoOpprinnelig
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val nyFakturaserieReferanse = postLagNyFakturaserieRequest(nyFakturaserieDto).expectStatus().isOk.expectBody(
            NyFakturaserieResponseDto::class.java
        ).returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        val nyFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(nyFakturaserieReferanse).shouldNotBeNull()
        val oppdatertOpprinneligFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse)


        oppdatertOpprinneligFakturaserie.shouldNotBeNull().erstattetMed!!.id shouldBe nyFakturaserie.id
        oppdatertOpprinneligFakturaserie.status shouldBe FakturaserieStatus.ERSTATTET
        oppdatertOpprinneligFakturaserie.faktura
            .map { it.status }
            .shouldContainExactlyInAnyOrder(
                FakturaStatus.BESTILT,
                FakturaStatus.AVBRUTT,
                FakturaStatus.AVBRUTT,
                FakturaStatus.AVBRUTT
            )

        nyFakturaserie.shouldNotBeNull().status shouldBe FakturaserieStatus.UNDER_BESTILLING
        nyFakturaserie.faktura
            .map { it.status }
            .shouldContainExactlyInAnyOrder(
                FakturaStatus.BESTILT,
                FakturaStatus.OPPRETTET,
                FakturaStatus.OPPRETTET,
                FakturaStatus.OPPRETTET
            )
        fakturaRepository.findByFakturaserieReferanse(nyFakturaserieReferanse)
            .single { it.status == FakturaStatus.BESTILT }
            .erAvregningsfaktura().shouldBeTrue()
    }

    @Test
    fun `erstatt fakturaserie hvor alt er i fortiden og bestilt - en avregningsfaktura pr faktura`() {
        val startDatoOpprinnelig = LocalDate.of(2016, 1, 1)
        val sluttDatoOpprinnelig = LocalDate.of(2017, 1, 31)

        val opprinneligFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    startDato = startDatoOpprinnelig
                    sluttDato = sluttDatoOpprinnelig
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk.expectBody(
                NyFakturaserieResponseDto::class.java
            ).returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        fakturaRepository.findByFakturaserieReferanse(opprinneligFakturaserieReferanse)
            .shouldNotBeNull()
            .shouldHaveSize(2)

        val nyFakturaserieDto = lagFakturaserieDto(
            referanseId = opprinneligFakturaserieReferanse, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 20000
                    startDato = startDatoOpprinnelig
                    sluttDato = sluttDatoOpprinnelig
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val nyFakturaserieReferanse = postLagNyFakturaserieRequest(nyFakturaserieDto).expectStatus().isOk.expectBody(
            NyFakturaserieResponseDto::class.java
        ).returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        val nyFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(nyFakturaserieReferanse).shouldNotBeNull()
        val oppdatertOpprinneligFakturaserie =
            fakturaserieRepositoryForTesting.findByReferanseEagerly(opprinneligFakturaserieReferanse)

        oppdatertOpprinneligFakturaserie.shouldNotBeNull().erstattetMed!!.id shouldBe nyFakturaserie.id
        oppdatertOpprinneligFakturaserie.status shouldBe FakturaserieStatus.ERSTATTET
        oppdatertOpprinneligFakturaserie.faktura
            .map { it.status }
            .shouldContainExactlyInAnyOrder(FakturaStatus.BESTILT, FakturaStatus.BESTILT)

        nyFakturaserie.shouldNotBeNull().status shouldBe FakturaserieStatus.UNDER_BESTILLING
        nyFakturaserie.faktura
            .shouldHaveSize(2)
            .forAll {
                it.status.shouldBe(FakturaStatus.BESTILT)
            }

        fakturaRepository.findByFakturaserieReferanse(nyFakturaserieReferanse)
            .shouldHaveSize(2)
            .forAll {
                it.krediteringFakturaRef.shouldNotBeNull()
                it.erAvregningsfaktura().shouldBeTrue()
            }
    }

    @Test
    fun `hent fakturaserier basert på referanseId`() {
        val startDato = LocalDate.parse("2023-01-01")
        val sluttDato = LocalDate.parse("2024-03-31")
        val fakturaSerieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    this.startDato = startDato
                    this.sluttDato = sluttDato
                    beskrivelse = "Inntekt fra utlandet"
                },
                FakturaseriePeriodeDto.forTest {
                    månedspris = 500
                    this.startDato = startDato
                    this.sluttDato = sluttDato
                    beskrivelse = "Misjonær"
                }
            )
        )

        val fakturaserieResponse1Referanse =
            postLagNyFakturaserieRequest(fakturaSerieDto).expectStatus().isOk.expectBody(NyFakturaserieResponseDto::class.java)
                .returnResult().responseBody!!.fakturaserieReferanse
        val fakturaserieResponse2Referanse = postLagNyFakturaserieRequest(fakturaSerieDto.apply {
            fakturaserieReferanse = fakturaserieResponse1Referanse
        }).expectStatus().isOk.expectBody(NyFakturaserieResponseDto::class.java)
            .returnResult().responseBody!!.fakturaserieReferanse
        val fakturaserieResponse3Referanse = postLagNyFakturaserieRequest(fakturaSerieDto.apply {
            fakturaserieReferanse = fakturaserieResponse2Referanse
        }).expectStatus().isOk.expectBody(NyFakturaserieResponseDto::class.java)
            .returnResult().responseBody!!.fakturaserieReferanse
        val fakturaserieResponse4Referanse = postLagNyFakturaserieRequest(fakturaSerieDto.apply {
            fakturaserieReferanse = fakturaserieResponse3Referanse
        }).expectStatus().isOk.expectBody(NyFakturaserieResponseDto::class.java)
            .returnResult().responseBody!!.fakturaserieReferanse

        val responseAlleFakturaserier = hentFakturaserierRequest(fakturaserieResponse4Referanse)
            .expectStatus().isOk
            .expectBodyList(FakturaserieResponseDto::class.java).returnResult().responseBody

        responseAlleFakturaserier?.size.shouldBe(4)
        responseAlleFakturaserier?.filter { it.status == FakturaserieStatus.ERSTATTET }?.size.shouldBe(3)
        responseAlleFakturaserier?.filter { it.status == FakturaserieStatus.OPPRETTET }?.size.shouldBe(1)
    }

    @Test
    fun `oppdater fakturamottaker, oppdaterer fullmektig på fakturaserie`() {
        val fakturaserieDto = lagFakturaserieDto(
            fullmektig = null,
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    startDato = LocalDate.now().minusMonths(3)
                    sluttDato = LocalDate.now().plusMonths(9)
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )
        val referanse =
            postLagNyFakturaserieRequest(fakturaserieDto).expectStatus().isOk.expectBody(
                NyFakturaserieResponseDto::class.java
            ).returnResult().responseBody!!.fakturaserieReferanse


        putOppdaterFakturaMottakerRequest(
            referanse,
            FakturamottakerRequestDto(FullmektigDto(null, "123123123"))
        ).expectStatus().isOk


        val oppdatertFakturaserie = fakturaserieRepositoryForTesting.findByReferanseEagerly(referanse).shouldNotBeNull()
        oppdatertFakturaserie.shouldNotBeNull().fullmektig.shouldBe(Fullmektig(null, "123123123"))

        oppdatertFakturaserie.endretAv.shouldBe(NAV_IDENT_ENDRING)
        oppdatertFakturaserie.endretTidspunkt.shouldNotBeNull()
    }

    @Test
    fun `lagNyFaktura med overlappende perioder er tillatt og resulterer i flere fakturalinjer på samme faktura`() {
        val startDato = LocalDate.parse("2023-01-01")
        val sluttDato = LocalDate.parse("2023-03-31")
        val fakturaSerieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    this.startDato = startDato
                    this.sluttDato = sluttDato
                    beskrivelse = "Inntekt fra utlandet"
                },
                FakturaseriePeriodeDto.forTest {
                    månedspris = 500
                    this.startDato = startDato
                    this.sluttDato = sluttDato
                    beskrivelse = "Misjonær"
                }
            )
        )

        val fakturaserieReferanse =
            postLagNyFakturaserieRequest(fakturaSerieDto).expectStatus().isOk.expectBody(NyFakturaserieResponseDto::class.java)
                .returnResult().responseBody!!.fakturaserieReferanse

        val response = hentFakturaserieRequest(fakturaserieReferanse)
            .expectStatus().isOk
            .expectBody(FakturaserieResponseDto::class.java).returnResult().responseBody

        response.shouldNotBeNull()
        response.faktura.size.shouldBe(1)
        response.faktura[0].fakturaLinje.map { it.periodeFra }.shouldContainOnly(startDato)
        response.faktura[0].fakturaLinje.map { it.periodeTil }.shouldContainOnly(sluttDato)
        response.faktura[0].fakturaLinje.map { it.beskrivelse }.shouldContainExactlyInAnyOrder(
            "Periode: 01.01.2023 - 31.03.2023\nInntekt fra utlandet",
            "Periode: 01.01.2023 - 31.03.2023\nMisjonær"
        )
    }

    @Test
    fun `lagNyFaktura lager ny fakturaserie med verdier utenfra`() {
        val startDato = LocalDate.parse("2023-01-01")
        val sluttDato = LocalDate.parse("2023-03-31")
        val fakturaSerieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    this.startDato = startDato
                    this.sluttDato = sluttDato
                    beskrivelse = "Inntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %"
                }
            )
        )

        val fakturaserieReferanse =
            postLagNyFakturaserieRequest(fakturaSerieDto).expectStatus().isOk.expectBody(NyFakturaserieResponseDto::class.java)
                .returnResult().responseBody!!.fakturaserieReferanse

        val response = hentFakturaserieRequest(fakturaserieReferanse)
            .expectStatus().isOk
            .expectBody(FakturaserieResponseDto::class.java).returnResult().responseBody

        response.shouldNotBeNull()
        response.faktura.size.shouldBe(1)
        response.faktura[0].fakturaLinje.map { it.periodeFra }.shouldContainOnly(startDato)
        response.faktura[0].fakturaLinje.map { it.periodeTil }.shouldContainOnly(sluttDato)
        response.faktura[0].fakturaLinje.map { it.beskrivelse }
            .shouldContainExactly("Periode: 01.01.2023 - 31.03.2023\nInntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %")
    }

    @Test
    fun `lagNyFaktura lager ny fakturaserie med sorterte fakturalinjer på periodeFra`() {
        val startDato = LocalDate.parse("2023-01-01")
        val sluttDato = LocalDate.parse("2023-03-31")
        val fakturaSerieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    this.startDato = startDato
                    this.sluttDato = sluttDato
                    beskrivelse = "Inntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %"
                },
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    this.startDato = startDato.plusDays(10)
                    this.sluttDato = sluttDato
                    beskrivelse = "Inntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %"
                },
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    this.startDato = startDato.plusDays(40)
                    this.sluttDato = sluttDato
                    beskrivelse = "Inntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %"
                },
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    this.startDato = startDato.plusDays(80)
                    this.sluttDato = sluttDato
                    beskrivelse = "Inntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %"
                },
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    this.startDato = startDato.plusDays(90)
                    this.sluttDato = sluttDato
                    beskrivelse = "Inntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %"
                },
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    this.startDato = startDato.plusDays(200)
                    this.sluttDato = sluttDato
                    beskrivelse = "Inntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %"
                }
            )
        )

        val fakturaserieReferanse =
            postLagNyFakturaserieRequest(fakturaSerieDto).expectStatus().isOk.expectBody(NyFakturaserieResponseDto::class.java)
                .returnResult().responseBody!!.fakturaserieReferanse

        val response = hentFakturaserieRequest(fakturaserieReferanse)
            .expectStatus().isOk
            .expectBody(FakturaserieResponseDto::class.java).returnResult().responseBody

        response.shouldNotBeNull()
        response.faktura.size.shouldBe(1)
        response.faktura[0].fakturaLinje.shouldHaveSize(4)
        val fakturaLinjer = response.faktura[0].fakturaLinje

        fakturaLinjer[0].beskrivelse.shouldBe("Periode: 22.03.2023 - 31.03.2023\nInntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %")
        fakturaLinjer[1].beskrivelse.shouldBe("Periode: 10.02.2023 - 31.03.2023\nInntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %")
        fakturaLinjer[2].beskrivelse.shouldBe("Periode: 11.01.2023 - 31.03.2023\nInntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %")
        fakturaLinjer[3].beskrivelse.shouldBe("Periode: 01.01.2023 - 31.03.2023\nInntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %")
    }

    @Test
    fun `kansellerFakturaserie kansellerer fakturaserie og returnerer ny fakturaseriereferanse`() {
        val opprinneligFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    startDato = LocalDate.now()
                    sluttDato = LocalDate.now().plusDays(2)
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto)
                .expectStatus().isOk
                .expectBody(NyFakturaserieResponseDto::class.java)
                .returnResult().responseBody!!.fakturaserieReferanse

        val nyFakturaserieReferanse = postKansellerFakturaserieRequest(opprinneligFakturaserieReferanse, emptyList())
            .expectStatus().isOk
            .expectBody(NyFakturaserieResponseDto::class.java)
            .returnResult().responseBody!!.fakturaserieReferanse

        nyFakturaserieReferanse shouldNotBe null
        nyFakturaserieReferanse shouldNotBe opprinneligFakturaserieReferanse
    }

    @ParameterizedTest(name = "{0} gir feilmelding \"{3}\"")
    @MethodSource("fakturaserieDTOsMedValideringsfeil")
    fun `lagNyFaktura validerer input riktig`(
        testbeskrivelse: String,
        fakturaserieRequestDto: FakturaserieRequestDto,
        validertFelt: String,
        feilmelding: String
    ) {
        postLagNyFakturaserieRequest(fakturaserieRequestDto)
            .expectStatus().isBadRequest
            .expectBody().json(
                """
                {
                    "status": 400,
                    "violations": [
                      {
                        "field": "$validertFelt",
                        "message": "$feilmelding"
                      }
                    ],
                    "title": "Constraint Violation"
                }
            """
            )
    }

    @Test
    fun `test fakturaserie, periode under 1 mnd skal gi korrekt dato for periode`() {
        val startDato = LocalDate.parse("2024-01-16")
        val sluttDato = LocalDate.parse("2024-01-31")
        val fakturaSerieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    this.startDato = startDato
                    this.sluttDato = sluttDato
                    beskrivelse = "Inntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %"
                }
            )
        )

        val fakturaserieReferanse =
            postLagNyFakturaserieRequest(fakturaSerieDto).expectStatus().isOk.expectBody(NyFakturaserieResponseDto::class.java)
                .returnResult().responseBody!!.fakturaserieReferanse

        val response = hentFakturaserieRequest(fakturaserieReferanse)
            .expectStatus().isOk
            .expectBody(FakturaserieResponseDto::class.java).returnResult().responseBody

        response.shouldNotBeNull()
            .faktura.shouldHaveSize(1).single()
            .fakturaLinje.shouldHaveSize(1).single()
            .beskrivelse.shouldBe("Periode: 16.01.2024 - 31.01.2024\nInntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %")
    }

    @Test
    fun `lag ny enkelt faktura (årsavregning)`() {
        val opprinneligFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 12000
                    startDato = LocalDate.of(2023, 1, 1)
                    sluttDato = LocalDate.of(2023, 12, 31)
                    beskrivelse = "Inntekt fra utlandet"
                }
            )
        )

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto)
                .expectStatus().isOk
                .expectBody(NyFakturaserieResponseDto::class.java)
                .returnResult().responseBody!!.fakturaserieReferanse

        val fakturaRequestDto = FakturaRequestDto(
            "12345678911",
            opprinneligFakturaserieReferanse,
            FullmektigDto("12345", "6789"),
            "referanseBruker",
            "referanseNAV",
            Innbetalingstype.TRYGDEAVGIFT,
            BigDecimal.valueOf(5000, 2),
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 12, 31),
            "beskrivelse"
        )


        val referanse = postLagNyFaktura(fakturaRequestDto)
            .expectStatus().isOk
            .expectBody(NyFakturaserieResponseDto::class.java)
            .returnResult().responseBody!!.fakturaserieReferanse

        val response = hentFakturaserieRequest(referanse)
            .expectStatus().isOk
            .expectBody(FakturaserieResponseDto::class.java).returnResult().responseBody


        response.shouldNotBeNull().run {
            intervall.shouldBe(FakturaserieIntervall.SINGEL)
            faktura.single().run {
                fakturaLinje.single().run {
                    belop.shouldBe(BigDecimal.valueOf(5000, 2))
                    beskrivelse.shouldBe("beskrivelse")
                }
            }
        }

    }

    //region fakturaserieDTOs med valideringsfeil
    private fun fakturaserieDTOsMedValideringsfeil(): List<Arguments> {
        return listOf(
            arguments(
                "Fødselsnummer med feil lengde",
                lagFakturaserieDto(fodselsnummer = "123456"),
                "fodselsnummer",
                "Fødselsnummeret er ikke gyldig"
            ),
            arguments(
                "Fødselsnummer med bokstaver",
                lagFakturaserieDto(fodselsnummer = "1234567891f"),
                "fodselsnummer",
                "Fødselsnummeret er ikke gyldig"
            ),
            arguments(
                "referanseNAV som er blank",
                lagFakturaserieDto(referanseNav = ""),
                "referanseNAV",
                "Du må oppgi referanseNAV"
            ),
            arguments(
                "Perioder som er tom",
                lagFakturaserieDto(fakturaseriePeriode = listOf()),
                "perioder",
                "Du må oppgi minst én periode"
            ),
        )
    }
    //endregion

    private fun postLagNyFakturaserieRequest(fakturaserieRequestDto: FakturaserieRequestDto): WebTestClient.ResponseSpec =
        webClient.post()
            .uri("/fakturaserier")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header("Nav-User-Id", NAV_IDENT)
            .bodyValue(fakturaserieRequestDto)
            .headers {
                it.set(HttpHeaders.CONTENT_TYPE, "application/json")
                it.set(HttpHeaders.AUTHORIZATION, "Bearer " + token())
            }
            .exchange()

    private fun putOppdaterFakturaMottakerRequest(
        referanse: String,
        fakturamottakerRequestDto: FakturamottakerRequestDto
    ): WebTestClient.ResponseSpec =
        webClient.put()
            .uri("/fakturaserier/{referanse}/mottaker", referanse)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .header("Nav-User-Id", NAV_IDENT_ENDRING)
            .bodyValue(fakturamottakerRequestDto)
            .headers {
                it.set(HttpHeaders.CONTENT_TYPE, "application/json")
                it.set(HttpHeaders.AUTHORIZATION, "Bearer " + token())
            }
            .exchange()

    private fun hentFakturaserieRequest(referanseId: String?): WebTestClient.ResponseSpec =
        webClient.get()
            .uri("/fakturaserier/$referanseId")
            .accept(MediaType.APPLICATION_JSON)
            .headers {
                it.set(HttpHeaders.CONTENT_TYPE, "application/json")
                it.set(HttpHeaders.AUTHORIZATION, "Bearer " + token())
            }
            .exchange()

    private fun hentFakturaserierRequest(referanse: String): WebTestClient.ResponseSpec =
        webClient.get()
            .uri("/fakturaserier?referanse=$referanse")
            .accept(MediaType.APPLICATION_JSON)
            .headers {
                it.set(HttpHeaders.CONTENT_TYPE, "application/json")
                it.set(HttpHeaders.AUTHORIZATION, "Bearer " + token())
            }
            .exchange()

    private fun postKansellerFakturaserieRequest(referanse: String, årsavregningRefs: List<String>): WebTestClient.ResponseSpec =
        webClient.post()
            .uri("/fakturaserier/$referanse/kanseller")
            .bodyValue(KanselleringRequestDto(årsavregningRefs))
            .accept(MediaType.APPLICATION_JSON)
            .header("Nav-User-Id", NAV_IDENT)
            .headers {
                it.set(HttpHeaders.CONTENT_TYPE, "application/json")
                it.set(HttpHeaders.AUTHORIZATION, "Bearer " + token())
            }
            .exchange()

    private fun postLagNyFaktura(fakturaRequestDto: FakturaRequestDto): WebTestClient.ResponseSpec =
        webClient.post()
            .uri("/fakturaer")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(fakturaRequestDto)
            .header("Nav-User-Id", NAV_IDENT)
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

    companion object {
        const val NAV_IDENT = "Z123456"
        const val NAV_IDENT_ENDRING = "T222222"

        fun lagFakturaserieDto(
            referanseId: String? = null,
            fodselsnummer: String = "12345678911",
            fullmektig: FullmektigDto? = FullmektigDto("11987654321", "123456789"),
            referanseBruker: String = "Nasse Nøff",
            referanseNav: String = "NAV referanse",
            fakturaGjelderInnbetalingstype: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
            intervall: FakturaserieIntervall = FakturaserieIntervall.KVARTAL,
            fakturaseriePeriode: List<FakturaseriePeriodeDto> = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 123
                    startDato = LocalDate.of(2022, 1, 1)
                    sluttDato = LocalDate.of(2022, 11, 30)
                    beskrivelse = "Beskrivelse"
                }
            ),
        ): FakturaserieRequestDto {
            return FakturaserieRequestDto(
                fodselsnummer,
                referanseId,
                fullmektig,
                referanseBruker,
                referanseNav,
                fakturaGjelderInnbetalingstype,
                intervall,
                fakturaseriePeriode
            )
        }
    }
}

/**
 * Oppretter dette interfacet for å ikke måtte bruke @Transactional på test metodene. Dette repoet skal kun brukes for
 * verifisering. Hvis noen finner en smart måte å kun kalle verifiserings logikken i en transaction så si gjerne ifra.
 */
interface FakturaserieRepositoryForTesting : JpaRepository<Fakturaserie, String> {

    @Query("SELECT fs FROM Fakturaserie fs JOIN fetch fs.faktura where fs.referanse = :referanse")
    fun findByReferanseEagerly(referanse: String): Fakturaserie?
}

interface FakturaRepositoryForControllerTesting : JpaRepository<Faktura, Long> {

    @Query("SELECT f FROM Faktura f JOIN fetch f.fakturaLinje WHERE f.id = :fakturaId")
    fun findByIdEagerly(fakturaId: Long): Faktura?
}
