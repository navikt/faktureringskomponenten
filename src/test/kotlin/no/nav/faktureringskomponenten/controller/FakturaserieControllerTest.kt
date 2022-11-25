package no.nav.faktureringskomponenten.controller

import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieIntervallDto
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.FullmektigDto
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
import org.assertj.core.internal.bytebuddy.utility.RandomString
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.*
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
class FakturaserieControllerTest(
    @Autowired val webClient: WebTestClient
) : PostgresTestContainerBase() {

    private val requestHeaders: MutableMap<String, String> = mutableMapOf()

    @AfterEach
    fun afterEach() {
        requestHeaders.clear()
    }

    @Test
    @ParameterizedTest(name = "{0} resulterer i {2}")
    @MethodSource("fakturaserieDTOs")
    fun `lagNyFaktura validerer input riktig`(
        testbeskrivelse: String,
        fakturaserieDto: FakturaserieDto,
        forventetStatus: HttpStatus
    ) {
        postLagNyFakturaRequest(fakturaserieDto).expectStatus().isEqualTo(forventetStatus)
    }

    private fun fakturaserieDTOs(): List<Arguments> {
        return listOf(
            arguments(
                "DTO med riktige verdier",
                lagFakturaserieDto(vedtaksnummer = "V1"),
                HttpStatus.OK
            ),
            arguments(
                "DTO med samme vedtaksId som tidligere",
                lagFakturaserieDto(vedtaksnummer = "V1"),
                HttpStatus.INTERNAL_SERVER_ERROR //TODO: Fiks errorhåndtering - https://jira.adeo.no/browse/MELOSYS-5543
            ),
            arguments(
                "Fødselsnummer med feil lengde",
                lagFakturaserieDto(fodselsnummer = "123456"),
                HttpStatus.BAD_REQUEST
            ),
            arguments(
                "Fødselsnummer med bokstaver",
                lagFakturaserieDto(fodselsnummer = "1234567891f"),
                HttpStatus.BAD_REQUEST
            ),
            arguments(
                "referanseNAV som er blank",
                lagFakturaserieDto(referanseNav = ""),
                HttpStatus.BAD_REQUEST
            ),
            arguments(
                "Perioder som er tom",
                lagFakturaserieDto(fakturaseriePeriode = listOf()),
                HttpStatus.BAD_REQUEST
            )
        )
    }

    private fun lagFakturaserieDto(
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
                LocalDate.now(),
                LocalDate.now(),
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

    private fun postLagNyFakturaRequest(fakturaserieDto: FakturaserieDto): ResponseSpec {
        return webClient.post()
            .uri("/fakturaserie")
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