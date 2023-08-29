package no.nav.faktureringskomponenten.controller

import com.nimbusds.jose.JOSEObjectType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import net.bytebuddy.utility.RandomString
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieRequestDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.dto.FullmektigDto
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.security.SubjectHandler.Companion.azureActiveDirectory
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Disabled
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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate

@Testcontainers
@ActiveProfiles("itest")
@AutoConfigureWebTestClient
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
class FakturaserieControllerTest(
    @Autowired private val webClient: WebTestClient,
    @Autowired private val server: MockOAuth2Server,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
) : PostgresTestContainerBase() {



    @Test
    @Disabled("Skal ikke støtte endring av fakturaserie i denne versjonen")
    fun `endre fakturaserie setter første faktura til å bli utsendt dagen etterpå`() {
        val vedtaksId = "id-3"
        val nyVedtaksId = "id-4"
        val startDatoOpprinnelig = LocalDate.now().minusMonths(3)
        val sluttDatoOpprinnelig = LocalDate.now().plusMonths(9)
        val startDatoNy = LocalDate.now().minusMonths(2)
        val sluttDatoNy = LocalDate.now().plusMonths(8)
        val opprinneligFakturaserieDto = lagFakturaserieDto(
            vedtaksId = vedtaksId, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(
                    BigDecimal.valueOf(123),
                    startDatoOpprinnelig,
                    sluttDatoOpprinnelig,
                    "Beskrivelse"
                )
            )
        )

        val nyFakturaserieDto = lagFakturaserieDto(
            vedtaksId = nyVedtaksId, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(
                    BigDecimal.valueOf(123),
                    startDatoNy,
                    sluttDatoNy,
                    "Beskrivelse"
                )
            )
        )

        postLagNyFakturaserieRequest(opprinneligFakturaserieDto)

        putEndreFakturaserieRequest(nyFakturaserieDto, vedtaksId)

        val nyFakturaserie = fakturaserieRepository.findByVedtaksId(nyVedtaksId).shouldNotBeNull()
        val oppdatertOpprinneligFakturaserie = fakturaserieRepository.findByVedtaksId(vedtaksId)

        oppdatertOpprinneligFakturaserie.shouldNotBeNull()
            .status.shouldBe(FakturaserieStatus.KANSELLERT)
        nyFakturaserie.startdato.shouldBe(startDatoNy)
        nyFakturaserie.sluttdato.shouldBe(sluttDatoNy)
    }

    @Test
    fun `hent fakturaserier basert på vedtaksId med fakturastatus filter`() {
        val saksnummer = "VEDTAK-1"
        val startDato = LocalDate.parse("2023-01-01")
        val sluttDato = LocalDate.parse("2024-03-31")
        val fakturaSerieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(BigDecimal(12000), startDato, sluttDato, "Inntekt fra utlandet"),
                FakturaseriePeriodeDto(BigDecimal(500), startDato, sluttDato, "Misjonær")
            )
        )
        addCleanUpAction {
            fakturaserieRepository.findByVedtaksId(fakturaSerieDto.vedtaksId)?.let {
                fakturaserieRepository.delete(it)
            }
        }


        postLagNyFakturaserieRequest(fakturaSerieDto).expectStatus().isOk

        val responseAlleFakturaOpprettet = hentFakturaserierRequest(saksnummer, FakturaStatus.OPPRETTET)
            .expectStatus().isOk
            .expectBodyList(FakturaserieResponseDto::class.java).returnResult().responseBody

        val fakturaserie = responseAlleFakturaOpprettet?.get(0)!!

        fakturaserie.faktura.size.shouldBe(3)
        fakturaserie.faktura[0].status.shouldBe(FakturaStatus.OPPRETTET)
        fakturaserie.faktura[1].status.shouldBe(FakturaStatus.OPPRETTET)
        fakturaserie.faktura[2].status.shouldBe(FakturaStatus.OPPRETTET)
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
        addCleanUpAction {
            fakturaserieRepository.findByVedtaksId(fakturaSerieDto.vedtaksId)?.let {
                fakturaserieRepository.delete(it)
            }
        }


        postLagNyFakturaserieRequest(fakturaSerieDto).expectStatus().isOk

        val response = hentFakturaserieRequest(fakturaSerieDto.vedtaksId)
            .expectStatus().isOk
            .expectBody(FakturaserieResponseDto::class.java).returnResult().responseBody

        response.shouldNotBeNull()
        response.faktura.size.shouldBe(1)
        response.faktura[0].fakturaLinje.map { it.periodeFra }.shouldContainOnly(startDato)
        response.faktura[0].fakturaLinje.map { it.periodeTil }.shouldContainOnly(sluttDato)
        response.faktura[0].fakturaLinje.map { it.beskrivelse }.shouldContainExactly("Inntekt fra utlandet", "Misjonær")
    }

    @Test
    fun `lagNyFaktura validerer duplikate vedtaksId`() {
        val duplikatNokkel = "id-1"
        addCleanUpAction {
            fakturaserieRepository.findByVedtaksId(duplikatNokkel)?.let {
                fakturaserieRepository.delete(it)
            }
        }

        val fakturaSerieDto = lagFakturaserieDto(vedtaksId = duplikatNokkel)
        postLagNyFakturaserieRequest(fakturaSerieDto).expectStatus().isOk

        postLagNyFakturaserieRequest(fakturaSerieDto)
            .expectStatus()
            .isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody()
            .jsonPath("$.violations.size()").isEqualTo(1)
            .jsonPath("$.violations[0].field").isEqualTo("vedtaksId")
            .jsonPath("$.violations[0].message").isEqualTo(
                "Kan ikke opprette fakturaserie når vedtaksId allerede finnes"
            )
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
        vedtaksId: String = "VEDTAK-1" + RandomString.make(3),
        fodselsnummer: String = "12345678911",
        fullmektig: FullmektigDto = FullmektigDto("11987654321", "123456789", "Ole Brum"),
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
            vedtaksId,
            fodselsnummer,
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

    private fun hentFakturaserieRequest(vedtaksId: String): WebTestClient.ResponseSpec =
        webClient.get()
            .uri("/fakturaserier/$vedtaksId")
            .accept(MediaType.APPLICATION_JSON)
            .headers {
                it.set(HttpHeaders.CONTENT_TYPE, "application/json")
                it.set(HttpHeaders.AUTHORIZATION, "Bearer " + token())
            }
            .exchange()

    private fun hentFakturaserierRequest(saksnummer: String, fakturaStatus: FakturaStatus?): WebTestClient.ResponseSpec =
        webClient.get()
            .uri("/fakturaserier?saksnummer=$saksnummer&fakturaStatus=${fakturaStatus.toString()}")
            .accept(MediaType.APPLICATION_JSON)
            .headers {
                it.set(HttpHeaders.CONTENT_TYPE, "application/json")
                it.set(HttpHeaders.AUTHORIZATION, "Bearer " + token())
            }
            .exchange()

    private fun putEndreFakturaserieRequest(
        fakturaserieRequestDto: FakturaserieRequestDto,
        gammelVedtaksId: String
    ): WebTestClient.ResponseSpec {
        return webClient.put()
            .uri("/fakturaserier/$gammelVedtaksId")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(fakturaserieRequestDto)
            .headers {
                it.set(HttpHeaders.CONTENT_TYPE, "application/json")
                it.set(HttpHeaders.AUTHORIZATION, "Bearer " + token())
            }
            .exchange()
    }

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
