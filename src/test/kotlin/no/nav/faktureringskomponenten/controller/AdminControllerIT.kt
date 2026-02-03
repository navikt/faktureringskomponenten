package no.nav.faktureringskomponenten.controller

import com.nimbusds.jose.JOSEObjectType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.faktureringskomponenten.controller.dto.*
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.cronjob.FakturaBestillCronjob
import no.nav.faktureringskomponenten.service.integration.kafka.EmbeddedKafkaBase
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
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate

@Testcontainers
@ActiveProfiles("itest", "embeded-kafka")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
class AdminControllerIT(
    @param:Autowired private val webClient: WebTestClient,
    @param:Autowired private val server: MockOAuth2Server,
    @param:Autowired private val fakturaserieRepositoryForTesting: FakturaserieRepositoryForTesting,
    @param:Autowired private val fakturaserieRepository: FakturaserieRepository,
    @param:Autowired private val fakturaRepository: FakturaRepository,
    @param:Autowired private val fakturaBestillCronjob: FakturaBestillCronjob,
) : EmbeddedKafkaBase(fakturaserieRepository) {

    @AfterEach
    fun cleanUp() {
        addCleanUpAction {
            fakturaserieRepositoryForTesting.deleteAll()
        }
    }

    @Test
    fun `hentAvstemmingCsv returnerer CSV med riktig header og data`() {
        val fakturaserieDto = lagFakturaserieDto()

        val fakturaserieReferanse = postLagNyFakturaserieRequest(fakturaserieDto)
            .expectStatus().isOk
            .expectBody<NyFakturaserieResponseDto>()
            .returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        val csvResponse = hentAvstemmingCsvRequest()
            .expectStatus().isOk
            .expectHeader().contentType("text/csv;charset=UTF-8")
            .expectBody<String>()
            .returnResult().responseBody!!

        val linjer = csvResponse.lines().filter { it.isNotBlank() }
        linjer.size shouldBe 2

        val header = linjer[0]
        header shouldContain "Fullmektig Organisasjonsnummer"
        header shouldContain "Dato Bestilt"
        header shouldContain "Totalbeløp"
        header shouldContain "Faktura Referanse"
        header shouldContain "Fakturaserie Referanse"

        val dataRad = linjer[1]
        dataRad.split(";").size shouldBe 5
        dataRad shouldContain "123456789"
        dataRad shouldContain fakturaserieReferanse
    }

    @Test
    fun `hentAvstemmingCsv returnerer tom CSV når ingen fakturaer finnes`() {
        val csvResponse = hentAvstemmingCsvRequest()
            .expectStatus().isOk
            .expectBody<String>()
            .returnResult().responseBody!!

        csvResponse.lines().filter { it.isNotBlank() }.size shouldBe 1
    }

    @Test
    fun `hentAvstemmingCsv returnerer kun header når fakturaer finnes utenfor perioden`() {
        val fakturaserieDto = lagFakturaserieDto()

        postLagNyFakturaserieRequest(fakturaserieDto)
            .expectStatus().isOk

        fakturaBestillCronjob.bestillFaktura()

        val csvResponse = hentAvstemmingCsvRequest(
            periodeFra = LocalDate.of(2020, 1, 1),
            periodeTil = LocalDate.of(2020, 12, 31)
        )
            .expectStatus().isOk
            .expectBody<String>()
            .returnResult().responseBody!!

        val linjer = csvResponse.lines().filter { it.isNotBlank() }
        linjer.size shouldBe 1
        linjer[0] shouldContain "Fullmektig Organisasjonsnummer"
    }

    @Test
    fun `ombestillFaktura med fakturaMottaker oppdaterer fullmektig i databasen`() {
        val fakturaserieDto = lagFakturaserieDto()

        val fakturaserieReferanse = postLagNyFakturaserieRequest(fakturaserieDto)
            .expectStatus().isOk
            .expectBody<NyFakturaserieResponseDto>()
            .returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        val fakturaserie = fakturaserieRepositoryForTesting.findByReferanseEagerly(fakturaserieReferanse)!!
        val fakturaReferanse = fakturaserie.faktura.first().referanseNr

        val faktura = fakturaRepository.findByReferanseNr(fakturaReferanse)!!
        faktura.status = FakturaStatus.FEIL
        fakturaRepository.saveAndFlush(faktura)

        val nyMottaker = "987654321"
        postOmbestillFakturaRequest(fakturaReferanse, nyMottaker)
            .expectStatus().isOk

        val oppdatertFakturaserie = fakturaserieRepositoryForTesting.findByReferanseEagerly(fakturaserieReferanse)!!
        oppdatertFakturaserie.fullmektig?.organisasjonsnummer shouldBe nyMottaker
    }

    @Test
    fun `ombestillFaktura uten fakturaMottaker endrer ikke fullmektig`() {
        val opprinneligMottaker = "123456789"
        val fakturaserieDto = lagFakturaserieDto(
            fullmektig = FullmektigDto(null, opprinneligMottaker)
        )

        val fakturaserieReferanse = postLagNyFakturaserieRequest(fakturaserieDto)
            .expectStatus().isOk
            .expectBody<NyFakturaserieResponseDto>()
            .returnResult().responseBody!!.fakturaserieReferanse

        fakturaBestillCronjob.bestillFaktura()

        val fakturaserie = fakturaserieRepositoryForTesting.findByReferanseEagerly(fakturaserieReferanse)!!
        val fakturaReferanse = fakturaserie.faktura.first().referanseNr

        val faktura = fakturaRepository.findByReferanseNr(fakturaReferanse)!!
        faktura.status = FakturaStatus.FEIL
        fakturaRepository.saveAndFlush(faktura)

        postOmbestillFakturaRequest(fakturaReferanse, null)
            .expectStatus().isOk

        val oppdatertFakturaserie = fakturaserieRepositoryForTesting.findByReferanseEagerly(fakturaserieReferanse)!!
        oppdatertFakturaserie.fullmektig?.organisasjonsnummer shouldBe opprinneligMottaker
    }

    private fun hentAvstemmingCsvRequest(
        periodeFra: LocalDate = LocalDate.of(2020, 1, 1),
        periodeTil: LocalDate = LocalDate.of(2030, 12, 31)
    ): WebTestClient.ResponseSpec =
        webClient.get()
            .uri("/admin/avstemming/csv?periodeFra=$periodeFra&periodeTil=$periodeTil")
            .headers {
                it.set(HttpHeaders.AUTHORIZATION, "Bearer " + token())
            }
            .exchange()

    private fun postOmbestillFakturaRequest(
        fakturaReferanse: String,
        fakturaMottaker: String?
    ): WebTestClient.ResponseSpec {
        val uri = if (fakturaMottaker != null) {
            "/admin/faktura/$fakturaReferanse/ombestill?fakturaMottaker=$fakturaMottaker"
        } else {
            "/admin/faktura/$fakturaReferanse/ombestill"
        }
        return webClient.post()
            .uri(uri)
            .header("Nav-User-Id", NAV_IDENT)
            .headers {
                it.set(HttpHeaders.AUTHORIZATION, "Bearer " + token())
            }
            .exchange()
    }

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

        fun lagFakturaserieDto(
            fodselsnummer: String = "12345678911",
            fullmektig: FullmektigDto? = FullmektigDto("11987654321", "123456789"),
            referanseBruker: String = "Nasse Nøff",
            referanseNav: String = "NAV referanse",
            fakturaGjelderInnbetalingstype: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
            intervall: FakturaserieIntervall = FakturaserieIntervall.KVARTAL,
            fakturaseriePeriode: List<FakturaseriePeriodeDto> = listOf(
                FakturaseriePeriodeDto.forTest {
                    månedspris = 1000
                    startDato = LocalDate.of(2023, 1, 1)
                    sluttDato = LocalDate.of(2023, 3, 31)
                    beskrivelse = "Test beskrivelse"
                }
            ),
        ): FakturaserieRequestDto {
            return FakturaserieRequestDto(
                fodselsnummer,
                null,
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
