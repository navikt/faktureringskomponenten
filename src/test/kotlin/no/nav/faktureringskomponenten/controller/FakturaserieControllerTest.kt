package no.nav.faktureringskomponenten.controller

import com.nimbusds.jose.JOSEObjectType
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import net.bytebuddy.utility.RandomString
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieRequestDto
import no.nav.faktureringskomponenten.controller.dto.FullmektigDto
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.models.FakturaserieTema
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

    @ParameterizedTest(name = "{0} gir feilmelding om overlappende perioder")
    @MethodSource("fakturaserieDTOsMedOverlappendePerioder")
    fun `lagNyFaktura validerer overlappende perioder riktig`(
        testbeskrivelse: String,
        fakturaserieRequestDto: FakturaserieRequestDto,
    ) {
        postLagNyFakturaserieRequest(fakturaserieRequestDto)
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.violations.size()").isEqualTo(1)
            .jsonPath("$.violations[0].field").isEqualTo("perioder")
            .jsonPath("$.violations[0].message").isEqualTo(
                "Periodene kan ikke overlappes"
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

    //region FakturaserieDTos med overlappende perioder
    private fun fakturaserieDTOsMedOverlappendePerioder(): List<Arguments> {
        return listOf(
            arguments(
                "Periode B starter midt i periode A",
                lagFakturaserieDto(
                    fakturaseriePeriode = lagOverlappendePerioder(
                        Pair(
                            LocalDate.of(2022, 1, 1),
                            LocalDate.of(2022, 4, 1)
                        ),
                        Pair(
                            LocalDate.of(2022, 3, 1),
                            LocalDate.of(2022, 6, 1)
                        ),
                    )
                ),
            ),
            arguments(
                "Periode A starter midt i periode B",
                lagFakturaserieDto(
                    fakturaseriePeriode = lagOverlappendePerioder(
                        Pair(
                            LocalDate.of(2022, 3, 1),
                            LocalDate.of(2022, 6, 1)
                        ),
                        Pair(
                            LocalDate.of(2022, 1, 1),
                            LocalDate.of(2022, 4, 1)
                        ),
                    )
                ),
            ),
            arguments(
                "Periode A starter og slutter innenfor periode B",
                lagFakturaserieDto(
                    fakturaseriePeriode = lagOverlappendePerioder(
                        Pair(
                            LocalDate.of(2022, 2, 1),
                            LocalDate.of(2022, 4, 1)
                        ),
                        Pair(
                            LocalDate.of(2022, 1, 1),
                            LocalDate.of(2022, 6, 1)
                        ),
                    )
                ),
            ),
            arguments(
                "Periode B starter og slutter innenfor periode A",
                lagFakturaserieDto(
                    fakturaseriePeriode = lagOverlappendePerioder(
                        Pair(
                            LocalDate.of(2022, 1, 1),
                            LocalDate.of(2022, 6, 1)
                        ),
                        Pair(
                            LocalDate.of(2022, 2, 1),
                            LocalDate.of(2022, 4, 1)
                        ),
                    )
                ),
            ),
            arguments(
                "Periode A slutter samme dag som periode B starter",
                lagFakturaserieDto(
                    fakturaseriePeriode = lagOverlappendePerioder(
                        Pair(
                            LocalDate.of(2022, 1, 1),
                            LocalDate.of(2022, 6, 1)
                        ),
                        Pair(
                            LocalDate.of(2022, 6, 1),
                            LocalDate.of(2022, 8, 1)
                        ),
                    )
                ),
            ),
            arguments(
                "Periode B slutter samme dag som periode A starter",
                lagFakturaserieDto(
                    fakturaseriePeriode = lagOverlappendePerioder(
                        Pair(
                            LocalDate.of(2022, 6, 1),
                            LocalDate.of(2022, 8, 1)
                        ),
                        Pair(
                            LocalDate.of(2022, 1, 1),
                            LocalDate.of(2022, 6, 1)
                        ),
                    )
                ),
            ),
            arguments(
                "Periode A og B er helt like",
                lagFakturaserieDto(
                    fakturaseriePeriode = lagOverlappendePerioder(
                        Pair(
                            LocalDate.of(2022, 1, 1),
                            LocalDate.of(2023, 8, 1)
                        ),
                        Pair(
                            LocalDate.of(2022, 1, 1),
                            LocalDate.of(2023, 8, 1)
                        ),
                    )
                ),
            ),
            arguments(
                "Én periode overlapper blant stor liste",
                lagFakturaserieDto(
                    fakturaseriePeriode = lagOverlappendePerioder(
                        Pair(
                            LocalDate.of(2022, 1, 1),
                            LocalDate.of(2022, 3, 31)
                        ),
                        Pair(
                            LocalDate.of(2022, 4, 1),
                            LocalDate.of(2022, 8, 30)
                        ),
                        Pair(
                            LocalDate.of(2022, 9, 1),
                            LocalDate.of(2023, 1, 1)
                        ),
                        Pair(
                            LocalDate.of(2023, 1, 1),
                            LocalDate.of(2023, 4, 1)
                        ),
                        Pair(
                            LocalDate.of(2023, 4, 2),
                            LocalDate.of(2023, 8, 1)
                        ),
                    )
                ),
            )
        )
    }
    //endregion


    fun lagFakturaserieDto(
        vedtaksId: String = "VEDTAK-1" + RandomString.make(3),
        fodselsnummer: String = "12345678911",
        fullmektig: FullmektigDto = FullmektigDto("11987654321", "123456789", "Ole Brum"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "NAV referanse",
        fakturaGjelder: FakturaserieTema = FakturaserieTema.TRY,
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
            fakturaGjelder,
            intervall,
            fakturaseriePeriode
        )
    }

    private fun lagOverlappendePerioder(vararg datoer: Pair<LocalDate, LocalDate>): List<FakturaseriePeriodeDto> {
        return datoer.map {
            FakturaseriePeriodeDto(
                BigDecimal.valueOf(123), it.first, it.second, "Beskrivelse"
            )
        }.toList()
    }

    private fun postLagNyFakturaserieRequest(fakturaserieRequestDto: FakturaserieRequestDto): WebTestClient.ResponseSpec =
        webClient.post()
            .uri("/fakturaserie")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(fakturaserieRequestDto)
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
            .uri("/fakturaserie/$gammelVedtaksId")
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
                mapOf(),
                3600
            )
        ).serialize()
}
