package no.nav.faktureringskomponenten.controller

import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
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
internal class FaktureringControllerTest(
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
        val faktureringPeriode1 = FakturaseriePeriodeDto(
            BigDecimal.valueOf(123),
            LocalDate.now(),
            LocalDate.now(),
            "Beskrivelse"
        )

        val faktureringDto = FakturaserieDto(
            "VEDTAK-1",
            "12345678911",
            null,
            "RefBruker",
            "refNAV",
            FaktureringsIntervall.KVARTAL,
            perioder = listOf(faktureringPeriode1)
        )

        postLagNyFakturaRequest(faktureringDto).expectStatus().isOk
    }

    @Test
    fun `lagNyFaktura feiler når fødselsnummer ikke har riktig lengde`() {
        val faktureringPeriode1 = FakturaseriePeriodeDto(
            BigDecimal.valueOf(123),
            LocalDate.now(),
            LocalDate.now(),
            "Beskrivelse"
        )

        val faktureringDto = FakturaserieDto(
            "VEDTAK-1",
            "123456789",
            null,
            "RefBruker",
            "refNAV",
            FaktureringsIntervall.KVARTAL,
            perioder = listOf(faktureringPeriode1)
        )

        postLagNyFakturaRequest(faktureringDto).expectStatus().is5xxServerError
    }

    private fun postLagNyFakturaRequest(faktureringDto: FakturaserieDto): ResponseSpec {
        return webClient.post()
            .uri("/fakturaserie")
            .bodyValue(faktureringDto)
            .headers(this::httpHeaders)
            .exchange()
    }

    private fun httpHeaders(httpHeaders: HttpHeaders) {
        requestHeaders.forEach {
            httpHeaders.add(it.key, it.value)
        }
    }
}