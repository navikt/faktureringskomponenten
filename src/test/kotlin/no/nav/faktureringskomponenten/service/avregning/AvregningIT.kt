package no.nav.faktureringskomponenten.service.avregning

import com.nimbusds.jose.JOSEObjectType
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieRequestDto
import no.nav.faktureringskomponenten.controller.dto.NyFakturaserieResponseDto
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
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
) : PostgresTestContainerBase() {
    @AfterEach
    fun cleanUp(){
        addCleanUpAction {
            fakturaserieRepository.deleteAll()
        }
    }
    
    @Test
    fun `erstatt fakturaserie med endringer tilbake i tid, sjekk avregning`() {
        // Opprinnelig serie
        val opprinneligFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(1000, "2024-01-01", "2024-12-31", "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"),
                FakturaseriePeriodeDto(2000, "2024-01-01", "2024-12-31", "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"),
            )
        )

        val opprinneligFakturaserieReferanse =
            postLagNyFakturaserieRequest(opprinneligFakturaserieDto).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
                .returnResult().responseBody!!.fakturaserieReferanse

        // Dette svarer til 2 fakturaer bestilt hos OEBS
        val opprinneligeFakturaer = fakturaRepository.findByFakturaserieReferanse(opprinneligFakturaserieReferanse).sortedBy(Faktura::getPeriodeFra)
        opprinneligeFakturaer[0].let {
            it.status = FakturaStatus.BESTILT
            it.eksternFakturaNummer = "8272123"
            fakturaRepository.save(it)
        }
        opprinneligeFakturaer[1].let {
            it.status = FakturaStatus.BESTILT
            it.eksternFakturaNummer = "8272124"
            fakturaRepository.save(it)
        }

        fakturaserieRepository.findByReferanse(opprinneligFakturaserieReferanse).let {
            it!!.status = FakturaserieStatus.UNDER_BESTILLING
            fakturaserieRepository.save(it)
        }

        // Serie 2 med avregning
        val fakturaserieDto2 = lagFakturaserieDto(
            referanseId = opprinneligFakturaserieReferanse,
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(1000, "2024-01-01", "2024-12-31", "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"),
                FakturaseriePeriodeDto(2000, "2024-01-01", "2024-02-29", "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"),
                FakturaseriePeriodeDto(3000, "2024-03-01", "2024-12-31", "Inntekt: 30000, Dekning: Pensjon og helsedel, Sats 10%"),
            )
        )

        val fakturaserieReferanse2 = postLagNyFakturaserieRequest(fakturaserieDto2).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
            .returnResult().responseBody!!.fakturaserieReferanse

        log.debug { "Tester 1. avregning" }
        val fakturaer2 = fakturaRepository.findByFakturaserieReferanse(fakturaserieReferanse2)
        val avregningsfaktura = fakturaer2.single { it.erAvregningsfaktura() }

        avregningsfaktura.fakturaLinje.sortedBy(FakturaLinje::periodeFra) shouldBe listOf(
            FakturaLinje(
                periodeFra = LocalDate.of(2024, 1, 1),
                periodeTil = LocalDate.of(2024, 3, 31),
                beskrivelse = "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 10000,00 - tidligere beløp: 9000,00",
                antall = BigDecimal("1.00"),
                enhetsprisPerManed = BigDecimal("1000.00"),
                avregningNyttBeloep = BigDecimal("10000.00"),
                avregningForrigeBeloep = BigDecimal("9000.00"),
                belop = BigDecimal("1000.00"),
            ),
            FakturaLinje(
                periodeFra = LocalDate.of(2024, 4, 1),
                periodeTil = LocalDate.of(2024, 6, 30),
                beskrivelse = "Periode: 01.04.2024 - 30.06.2024\nNytt beløp: 12000,00 - tidligere beløp: 9000,00",
                antall = BigDecimal("1.00"),
                enhetsprisPerManed = BigDecimal("3000.00"),
                avregningNyttBeloep = BigDecimal("12000.00"),
                avregningForrigeBeloep = BigDecimal("9000.00"),
                belop = BigDecimal("3000.00"),
            ),
        )

        // Bestiller avregningsfaktura og 1 faktura fra 2. serie
        avregningsfaktura.let {
            it.status = FakturaStatus.BESTILT
            it.eksternFakturaNummer = "1234"
            fakturaRepository.save(it)
        }
        fakturaer2[0].let {
            it.status = FakturaStatus.BESTILT
            it.eksternFakturaNummer = "12345"
            fakturaRepository.save(it)
        }
        fakturaserieRepository.findByReferanse(fakturaserieReferanse2).let {
            it!!.status = FakturaserieStatus.UNDER_BESTILLING
            fakturaserieRepository.save(it)
        }

        // Serie 3 med avregning
        val fakturaserieDto3 = lagFakturaserieDto(
            referanseId = fakturaserieReferanse2,
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(1000, "2024-01-01", "2024-12-31", "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"),
                FakturaseriePeriodeDto(2000, "2024-01-01", "2024-01-31", "Inntekt: 20000, Dekning: Pensjon og helsedel, Sats 10%"),
                FakturaseriePeriodeDto(3000, "2024-02-01", "2024-12-31", "Inntekt: 30000, Dekning: Pensjon og helsedel, Sats 10%"),
            )
        )

        val serieRef3 = postLagNyFakturaserieRequest(fakturaserieDto3).expectStatus().isOk.expectBody<NyFakturaserieResponseDto>()
            .returnResult().responseBody!!.fakturaserieReferanse

        log.debug { "Tester 2. avregning" }
        fakturaRepository.findByFakturaserieReferanse(serieRef3).single { it.erAvregningsfaktura() }
            .fakturaLinje.shouldHaveSize(1).first() shouldBe
                FakturaLinje(
                    referertFakturaVedAvregning = null, //bør testes
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