package no.nav.faktureringskomponenten.controller

import com.nimbusds.jose.JOSEObjectType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.controller.dto.*
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.security.SubjectHandler.Companion.azureActiveDirectory
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
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
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate

@Testcontainers
@ActiveProfiles("itest")
@AutoConfigureWebTestClient
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
class FakturaserieControllerIT(
    @Autowired private val webClient: WebTestClient,
    @Autowired private val server: MockOAuth2Server,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
) : PostgresTestContainerBase() {

    @AfterEach
    fun cleanUp() {
        addCleanUpAction {
            fakturaserieRepository.deleteAll()
        }
    }

    @Test
    @Transactional
    fun `erstatt fakturaserie, erstatter opprinnelig og lager ny`() {
        val startDatoOpprinnelig = LocalDate.now().minusMonths(3)
        val sluttDatoOpprinnelig = LocalDate.now().plusMonths(9)
        val startDatoNy = LocalDate.now().minusMonths(2)
        val sluttDatoNy = LocalDate.now().plusMonths(8)

        val opprinneligFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(
                    BigDecimal(12000),
                    startDatoOpprinnelig,
                    sluttDatoOpprinnelig,
                    "Inntekt fra utlandet"
                ),
            )
        )

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk.expectBody(
                NyFakturaserieResponseDto::class.java
            ).returnResult().responseBody!!.fakturaserieReferanse

        val nyFakturaserieDto = lagFakturaserieDto(
            referanseId = opprinneligFakturaserieReferanse, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(BigDecimal(18000), startDatoNy, sluttDatoNy, "Inntekt fra Norge"),
                FakturaseriePeriodeDto(BigDecimal(24000), startDatoNy, sluttDatoNy, "Inntekt fra utlandet"),
            )
        )

        val nyFakturaserieReferanse = postLagNyFakturaserieRequest(nyFakturaserieDto).expectStatus().isOk.expectBody(
            NyFakturaserieResponseDto::class.java
        ).returnResult().responseBody!!.fakturaserieReferanse


        val nyFakturaserie = fakturaserieRepository.findByReferanse(nyFakturaserieReferanse).shouldNotBeNull()
        val oppdatertOpprinneligFakturaserie = fakturaserieRepository.findByReferanse(opprinneligFakturaserieReferanse)

        oppdatertOpprinneligFakturaserie.shouldNotBeNull().erstattetMed!!.id shouldBe nyFakturaserie.id
        oppdatertOpprinneligFakturaserie.status shouldBe FakturaserieStatus.ERSTATTET
        oppdatertOpprinneligFakturaserie.faktura.forEach {
            it.status.shouldBe(FakturaStatus.KANSELLERT)
        }

        nyFakturaserie.startdato.shouldBe(startDatoNy)
        nyFakturaserie.sluttdato shouldBe sluttDatoNy
        nyFakturaserie.shouldNotBeNull().status shouldBe FakturaserieStatus.OPPRETTET
        nyFakturaserie.faktura.forEach {
            it.status shouldBe FakturaStatus.OPPRETTET
        }
    }

    @Test
    fun `hent fakturaserier basert på referanseId`() {
        val startDato = LocalDate.parse("2023-01-01")
        val sluttDato = LocalDate.parse("2024-03-31")
        val fakturaSerieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(BigDecimal(12000), startDato, sluttDato, "Inntekt fra utlandet"),
                FakturaseriePeriodeDto(BigDecimal(500), startDato, sluttDato, "Misjonær")
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
    @Transactional
    fun `oppdater fakturamottaker, oppdaterer fullmektig på fakturaserie`() {
        val fakturaserieDto = lagFakturaserieDto(
            fullmektig = null,
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(
                    BigDecimal(12000),
                    LocalDate.now().minusMonths(3),
                    LocalDate.now().plusMonths(9),
                    "Inntekt fra utlandet"
                ),
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


        val oppdatertFakturaserie = fakturaserieRepository.findByReferanse(referanse).shouldNotBeNull()
        oppdatertFakturaserie.shouldNotBeNull().fullmektig.shouldBe(Fullmektig(null, "123123123"))
    }

    @Test
    fun `lagNyFaktura med overlappende perioder er tillatt og resulterer i flere fakturalinjer på samme faktura`() {
        val startDato = LocalDate.parse("2023-01-01")
        val sluttDato = LocalDate.parse("2023-03-31")
        val fakturaSerieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(BigDecimal(12000), startDato, sluttDato, "Inntekt fra utlandet"),
                FakturaseriePeriodeDto(BigDecimal(500), startDato, sluttDato, "Misjonær")
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
        response.faktura[0].fakturaLinje.map { it.beskrivelse }.shouldContainExactly("Periode: 01.01.2023 - 31.03.2023\nInntekt fra utlandet", "Periode: 01.01.2023 - 31.03.2023\nMisjonær")
    }

    @Test
    fun `lagNyFaktura lager ny fakturaserie med verdier utenfra`() {
        val startDato = LocalDate.parse("2023-01-01")
        val sluttDato = LocalDate.parse("2023-03-31")
        val fakturaSerieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(BigDecimal(12000), startDato, sluttDato, "Inntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %"),
            )
        )

        val fakturaserieReferanse = postLagNyFakturaserieRequest(fakturaSerieDto).expectStatus().isOk.expectBody(NyFakturaserieResponseDto::class.java).returnResult().responseBody!!.fakturaserieReferanse

        val response = hentFakturaserieRequest(fakturaserieReferanse)
            .expectStatus().isOk
            .expectBody(FakturaserieResponseDto::class.java).returnResult().responseBody

        response.shouldNotBeNull()
        response.faktura.size.shouldBe(1)
        response.faktura[0].fakturaLinje.map { it.periodeFra }.shouldContainOnly(startDato)
        response.faktura[0].fakturaLinje.map { it.periodeTil }.shouldContainOnly(sluttDato)
        response.faktura[0].fakturaLinje.map { it.beskrivelse }.shouldContainExactly("Periode: 01.01.2023 - 31.03.2023\nInntekt: 5000.0, Dekning: Helse- og pensjonsdel med syke- og foreldrepenger (§ 2-9), Sats: 3.5 %")
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

    fun lagFakturaserieDto(
        referanseId: String? = null,
        fodselsnummer: String = "12345678911",
        fullmektig: FullmektigDto? = FullmektigDto("11987654321", "123456789"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "NAV referanse",
        fakturaGjelderInnbetalingstype: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
        intervall: FakturaserieIntervall = FakturaserieIntervall.KVARTAL,
        fakturaseriePeriode: List<FakturaseriePeriodeDto> = listOf(
            FakturaseriePeriodeDto(
                BigDecimal.valueOf(123),
                LocalDate.of(2022, 1, 1),
                LocalDate.of(2022, 11, 30),
                "Beskrivelse"
            )
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

    private fun postLagNyFakturaserieRequest(fakturaserieRequestDto: FakturaserieRequestDto): WebTestClient.ResponseSpec =
        webClient.post()
            .uri("/fakturaserier")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
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

    private fun token(subject: String = "faktureringskomponenten-test"): String? =
        server.issueToken(
            azureActiveDirectory,
            "faktureringskomponenten-test",
            DefaultOAuth2TokenCallback(
                azureActiveDirectory,
                subject,
                JOSEObjectType.JWT.type,
                listOf("faktureringskomponenten-localhost"),
                mapOf("roles" to "faktureringskomponenten-skriv"),
                3600
            )
        ).serialize()
}
