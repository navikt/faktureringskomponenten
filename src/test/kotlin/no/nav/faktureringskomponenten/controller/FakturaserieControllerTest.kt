package no.nav.faktureringskomponenten.controller

import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieIntervallDto
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.FullmektigDto
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.FakturaService
import no.nav.faktureringskomponenten.service.FakturaserieService
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
import org.assertj.core.internal.bytebuddy.utility.RandomString
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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate

@Testcontainers
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FakturaserieControllerTest(
    @Autowired val webClient: WebTestClient,
    @Autowired val fakturaserieRepository: FakturaserieRepository,
    @Autowired val fakturaserieService: FakturaserieService,
    @Autowired val fakturaService: FakturaService
) : PostgresTestContainerBase() {

    private val requestHeaders: MutableMap<String, String> = mutableMapOf()

    @AfterEach
    fun afterEach() {
        requestHeaders.clear()
    }


    @Test
    fun `endre fakturaserie lager ikke kopi av bestilte fakturaer`() {
        val vedtaksId = "id-100"
        val startDatoOpprinnelig = LocalDate.now().minusMonths(4)
        val sluttDatoOpprinnelig = LocalDate.now().plusMonths(8)

        val opprinneligFakturaserieDto = lagFakturaserieDto(
            vedtaksnummer = vedtaksId, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(
                    BigDecimal.valueOf(123),
                    startDatoOpprinnelig,
                    sluttDatoOpprinnelig,
                    "Beskrivelse"
                )
            )
        )

        val nyVedtaksId = "id-101"
        val nyStartDato = LocalDate.now().minusMonths(3)
        val nySluttDato = LocalDate.now().plusMonths(7)
        val nyFakturaserieDto = lagFakturaserieDto(
            vedtaksnummer = nyVedtaksId, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(
                    BigDecimal.valueOf(123),
                    nyStartDato,
                    nySluttDato,
                    "Beskrivelse"
                )
            )
        )

        postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk

        fakturaserieService.bestillFakturaserie(vedtaksId, LocalDate.now().plusDays(10))

        putEndreFakturaserieRequest(nyFakturaserieDto, vedtaksId).expectStatus().isOk

        val nyFakturaserie = fakturaserieRepository.findByVedtaksId(nyVedtaksId).get()
        val oppdatertOpprinneligFakturaserie = fakturaserieRepository.findByVedtaksId(vedtaksId).get()

        oppdatertOpprinneligFakturaserie.status.shouldBe(FakturaserieStatus.KANSELLERT)
        nyFakturaserie.status.shouldBe(FakturaserieStatus.OPPRETTET)
        nyFakturaserie.startdato.shouldBe(LocalDate.now().plusMonths(1).withDayOfMonth(1))
        nyFakturaserie.sluttdato.shouldBe(nySluttDato)
    }

    @Test
    fun `endre fakturaserie setter første faktura til å bli utsendt dagen etterpå`() {
        val vedtaksId = "id-3"
        val nyVedtaksId = "id-4"
        val startDatoOpprinnelig = LocalDate.now().minusMonths(3)
        val sluttDatoOpprinnelig = LocalDate.now().plusMonths(9)
        val startDatoNy = LocalDate.now().minusMonths(2)
        val sluttDatoNy = LocalDate.now().plusMonths(8)
        val opprinneligFakturaserieDto = lagFakturaserieDto(
            vedtaksnummer = vedtaksId, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(
                    BigDecimal.valueOf(123),
                    startDatoOpprinnelig,
                    sluttDatoOpprinnelig,
                    "Beskrivelse"
                )
            )
        )

        val nyFakturaserieDto = lagFakturaserieDto(
            vedtaksnummer = nyVedtaksId, fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(
                    BigDecimal.valueOf(123),
                    startDatoNy,
                    sluttDatoNy,
                    "Beskrivelse"
                )
            )
        )

        postLagNyFakturaserieRequest(opprinneligFakturaserieDto)
        val bestillingsKlareFaktura = fakturaService.hentBestillingsklareFaktura(LocalDate.now().plusDays(10))
        bestillingsKlareFaktura.size.shouldBe(1)

        putEndreFakturaserieRequest(nyFakturaserieDto, vedtaksId)

        val nyFakturaserie = fakturaserieRepository.findByVedtaksId(nyVedtaksId).get()
        val oppdatertOpprinneligFakturaserie = fakturaserieRepository.findByVedtaksId(vedtaksId).get()
        val bestillingsKlareFakturaNy = fakturaService.hentBestillingsklareFaktura(LocalDate.now().plusDays(10))

        oppdatertOpprinneligFakturaserie.status.shouldBe(FakturaserieStatus.KANSELLERT)
        bestillingsKlareFakturaNy.size.shouldBe(1)
        nyFakturaserie.startdato.shouldBe(startDatoNy)
        nyFakturaserie.sluttdato.shouldBe(sluttDatoNy)
    }

    @Test
    fun `lagNyFaktura validerer duplikate vedtaksId`() {
        val duplikatNokkel = "id-1"

        lagFakturaserieDto(vedtaksnummer = duplikatNokkel)
        postLagNyFakturaserieRequest(lagFakturaserieDto(vedtaksnummer = duplikatNokkel)).expectStatus().isOk

        postLagNyFakturaserieRequest(lagFakturaserieDto(vedtaksnummer = duplikatNokkel))
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
        fakturaserieDto: FakturaserieDto,
        validertFelt: String,
        feilmelding: String
    ) {
        postLagNyFakturaserieRequest(fakturaserieDto)
            .expectStatus().isBadRequest
            .expectBody().json(
                """
                {
                    "type": "https://zalando.github.io/problem/constraint-violation",
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
        fakturaserieDto: FakturaserieDto,
    ) {
        postLagNyFakturaserieRequest(fakturaserieDto)
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
                "fakturaGjelder som er tom",
                lagFakturaserieDto(fakturaGjelder = ""),
                "fakturaGjelder",
                "Du må oppgi fakturaGjelder"
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
        vedtaksnummer: String = "VEDTAK-1" + RandomString.make(3),
        fodselsnummer: String = "12345678911",
        fullmektig: FullmektigDto = FullmektigDto("11987654321", "123456789", "Ole Brum"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "NAV referanse",
        fakturaGjelder: String = "Trygdeavgift",
        intervall: FakturaserieIntervallDto = FakturaserieIntervallDto.KVARTAL,
        fakturaseriePeriode: List<FakturaseriePeriodeDto> = listOf(
            FakturaseriePeriodeDto(
                BigDecimal.valueOf(123),
                LocalDate.of(2022, 1, 1),
                LocalDate.of(2022, 11, 30),
                "Beskrivelse"
            )
        ),
    ): FakturaserieDto {
        return FakturaserieDto(
            vedtaksnummer,
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

    private fun postLagNyFakturaserieRequest(fakturaserieDto: FakturaserieDto): WebTestClient.ResponseSpec {
        return webClient.post()
            .uri("/fakturaserie")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(fakturaserieDto)
            .headers(this::httpHeaders)
            .exchange()
    }

    private fun putEndreFakturaserieRequest(
        fakturaserieDto: FakturaserieDto,
        gammelVedtaksId: String
    ): WebTestClient.ResponseSpec {
        return webClient.put()
            .uri("/fakturaserie/$gammelVedtaksId")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(fakturaserieDto)
            .headers(this::httpHeaders)
            .exchange()
    }

    private fun httpHeaders(httpHeaders: HttpHeaders) {
        requestHeaders.forEach {
            httpHeaders.add(it.key, it.value)
        }
    }
}