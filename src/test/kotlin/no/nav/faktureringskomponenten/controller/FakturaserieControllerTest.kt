package no.nav.faktureringskomponenten.controller

import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.controller.dto.FullmektigDto
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.FaktureringsIntervall
import no.nav.faktureringskomponenten.testutils.PostgresContainer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate

@ActiveProfiles("test")
@Testcontainers
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class FakturaserieControllerTest(
    @Autowired val webClient: WebTestClient
) {

    companion object {
        @Container
        val testContainer: PostgresContainer = PostgresContainer.setupAndStart()
    }

    private val requestHeaders: MutableMap<String, String> = mutableMapOf()

    @AfterEach
    fun afterEach() {
        requestHeaders.clear()
    }

    @Test
    fun `lagNyFaktura lager ny faktura når alle verdier er riktige`() {
        val fakturaserieDto = lagFakturaserieDto()

        postLagNyFakturaRequest(fakturaserieDto)
            .expectStatus().isOk
    }

    @Test
    fun `lagNyFaktura feiler når fødselsnummer ikke har riktig lengde`() {
        val fakturaserieDto = lagFakturaserieDto(fodselsnummer = "123456")

        postLagNyFakturaRequest(fakturaserieDto)
            .expectStatus().is4xxClientError
    }

    @Test
    fun `lagNyFaktura feiler når fødselsnummer ikke er sifre`() {
        val fakturaserieDto = lagFakturaserieDto(fodselsnummer = "1234567891f")

        postLagNyFakturaRequest(fakturaserieDto)
            .expectStatus().is4xxClientError
    }

    @Test
    fun `lagNyFaktura feiler når referanseNAV er blank`() {
        val fakturaserieDto = lagFakturaserieDto(referanseNav = "")

        postLagNyFakturaRequest(fakturaserieDto)
            .expectStatus().is4xxClientError
    }

    @Test
    fun `lagNyFaktura feiler når perioder er tom`() {
        val fakturaserieDto = lagFakturaserieDto(fakturaseriePeriode = listOf())

        postLagNyFakturaRequest(fakturaserieDto)
            .expectStatus().is4xxClientError
    }

    private fun lagFakturaserieDto(
        vedtaksnummer: String = "VEDTAK-1",
        fodselsnummer: String = "12345678911",
        fullmektig: FullmektigDto = FullmektigDto("11987654321", "123456789", "Ole Brum"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "NAV referanse",
        intervall: FaktureringsIntervall = FaktureringsIntervall.KVARTAL,
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