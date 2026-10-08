package no.nav.faktureringskomponenten.controller

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.config.AdminTilgangInterceptor
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.EmbeddedKafkaBase
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ActiveProfiles("itest", "embeded-kafka")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
class AdminTilgangsstyringIT(
    @param:Autowired private val webClient: WebTestClient,
    @param:Autowired private val server: MockOAuth2Server,
    @param:Autowired private val fakturaserieRepository: FakturaserieRepository,
    @param:Value("\${admin.driftsgruppe}") private val driftsgruppeId: String,
    @param:Value("\${admin.console-klient-id}") private val consoleKlientId: String,
    @param:Autowired @param:Qualifier("requestMappingHandlerMapping")
    private val handlerMapping: RequestMappingHandlerMapping,
) : EmbeddedKafkaBase(fakturaserieRepository) {

    @Test
    fun `personkall fra Console med driftsgruppe får tilgang`() {
        hentMottakFeil(personToken(grupper = listOf(driftsgruppeId))).expectStatus().isOk
    }

    @Test
    fun `maskinkall fra Console får tilgang`() {
        hentMottakFeil(maskinToken()).expectStatus().isOk
    }

    @Test
    fun `personkall fra annen klient avvises, selv med driftsgruppe`() {
        hentMottakFeil(personToken(grupper = listOf(driftsgruppeId), klientId = MELOSYS_WEB))
            .expectStatus().isForbidden
            .expectBody(String::class.java).isEqualTo(AdminTilgangInterceptor.UKJENT_KLIENT)
    }

    @Test
    fun `maskinkall fra annen klient avvises`() {
        hentMottakFeil(maskinToken(klientId = MELOSYS_API))
            .expectStatus().isForbidden
            .expectBody(String::class.java).isEqualTo(AdminTilgangInterceptor.UKJENT_KLIENT)
    }

    @Test
    fun `personkall uten driftsgruppe avvises med forklaring`() {
        hentMottakFeil(personToken(grupper = listOf(ANNEN_GRUPPE)))
            .expectStatus().isForbidden
            .expectBody(String::class.java).isEqualTo(AdminTilgangInterceptor.MANGLER_DRIFTSGRUPPE)
    }

    @Test
    fun `personkall uten groups-claim avvises`() {
        hentMottakFeil(personToken(grupper = null))
            .expectStatus().isForbidden
            .expectBody(String::class.java).isEqualTo(AdminTilgangInterceptor.MANGLER_DRIFTSGRUPPE)
    }

    @Test
    fun `token med annen idtyp enn app regnes som personkall`() {
        hentMottakFeil(token(mapOf("idtyp" to "user")))
            .expectStatus().isForbidden
            .expectBody(String::class.java).isEqualTo(AdminTilgangInterceptor.MANGLER_DRIFTSGRUPPE)
    }

    @Test
    fun `kall uten token avvises`() {
        hentMottakFeil(token = null)
            .expectStatus().isUnauthorized
            .expectBody(String::class.java).isEqualTo(AdminTilgangInterceptor.MANGLER_TOKEN)
    }

    @Test
    fun `token med feil audience avvises`() {
        hentMottakFeil(token(mapOf("groups" to listOf(driftsgruppeId)), audience = "annen-app"))
            .expectStatus().isUnauthorized
    }

    @Test
    fun `ruter utenfor admin påvirkes ikke`() {
        // melosys-web henter fakturaserier med saksbehandlers token, uten vaktgruppen
        webClient.get()
            .uri("/fakturaserier/finnes-ikke")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + personToken(grupper = emptyList(), klientId = MELOSYS_WEB))
            .exchange()
            .expectStatus().isNotFound
    }

    // --- Alle registrerte admin-endepunkter ---
    //
    // Endepunktene hentes fra Spring, så nye admin-endepunkter dekkes uten at testene må oppdateres.
    // assertSoftly viser alle endepunkter som feiler, ikke bare det første.

    @Test
    fun `kall uten token avvises på alle registrerte admin-endepunkter`() {
        val endepunkter = registrerteAdminEndepunkter()

        assertSoftly {
            endepunkter.forEach { endepunkt ->
                withClue(endepunkt) {
                    kall(endepunkt, token = null).returnResult(String::class.java).status.value() shouldBe 401
                }
            }
        }
    }

    @Test
    fun `kall fra annen klient avvises på alle registrerte admin-endepunkter`() {
        val endepunkter = registrerteAdminEndepunkter()
        val token = personToken(grupper = listOf(driftsgruppeId), klientId = MELOSYS_WEB)

        assertSoftly {
            endepunkter.forEach { endepunkt ->
                withClue(endepunkt) {
                    val respons = kall(endepunkt, token).returnResult(String::class.java)
                    respons.status.value() shouldBe 403
                    respons.responseBody.blockFirst() shouldBe AdminTilgangInterceptor.UKJENT_KLIENT
                }
            }
        }
    }

    @Test
    fun `personkall uten driftsgruppe avvises på alle registrerte admin-endepunkter`() {
        val endepunkter = registrerteAdminEndepunkter()
        val token = personToken(grupper = emptyList())

        assertSoftly {
            endepunkter.forEach { endepunkt ->
                withClue(endepunkt) {
                    val respons = kall(endepunkt, token).returnResult(String::class.java)
                    respons.status.value() shouldBe 403
                    respons.responseBody.blockFirst() shouldBe AdminTilgangInterceptor.MANGLER_DRIFTSGRUPPE
                }
            }
        }
    }

    private data class Endepunkt(val metode: String, val mønster: String) {
        // Interceptoren avviser før argumentene leses, så stivariablene trenger bare å matche mønsteret
        val sti = mønster.replace(Regex("\\{[^}]+}"), "1")

        override fun toString() = "$metode $mønster"
    }

    private fun registrerteAdminEndepunkter(): List<Endepunkt> {
        val endepunkter = handlerMapping.handlerMethods.keys.flatMap { info ->
            val metoder = info.methodsCondition.methods.ifEmpty { setOf(RequestMethod.GET) }
            info.patternValues
                .filter { it.startsWith("/admin/") }
                .flatMap { mønster -> metoder.map { Endepunkt(it.name, mønster) } }
        }

        // Vakt mot falsk grønn: finner oppslaget ingen endepunkter, kjører forEach ingen assertions,
        // og testene passerer uten å ha sjekket noe. Ett GET- og ett PUT-endepunkt viser at flere metoder dekkes.
        endepunkter.map { it.toString() }.shouldContainAll(
            "GET /admin/faktura/mottak/feil",
            "PUT /admin/fakturaserie/{referanse}/fnr",
        )
        return endepunkter
    }

    private fun kall(endepunkt: Endepunkt, token: String?): WebTestClient.ResponseSpec =
        webClient.method(HttpMethod.valueOf(endepunkt.metode))
            .uri(endepunkt.sti)
            // AuditorAwareFilter krever ident på POST og PUT og kjører før interceptoren
            .header(AuditorAwareFilter.NAV_USER_ID, NAV_IDENT)
            .headers { headers -> token?.let { headers.set(HttpHeaders.AUTHORIZATION, "Bearer $it") } }
            .exchange()

    private fun hentMottakFeil(token: String?): WebTestClient.ResponseSpec =
        webClient.get()
            .uri("/admin/faktura/mottak/feil")
            .headers { headers -> token?.let { headers.set(HttpHeaders.AUTHORIZATION, "Bearer $it") } }
            .exchange()

    private fun personToken(grupper: List<String>?, klientId: String = consoleKlientId): String =
        token(
            buildMap {
                put("NAVident", NAV_IDENT)
                grupper?.let { put("groups", it) }
            },
            klientId = klientId,
        )

    private fun maskinToken(klientId: String = consoleKlientId): String =
        token(mapOf("idtyp" to "app"), klientId = klientId)

    // mock-oauth2-server setter azp til klient-ID-en tokenet utstedes til
    private fun token(
        claims: Map<String, Any>,
        klientId: String = consoleKlientId,
        audience: String = "faktureringskomponenten-localhost",
    ): String =
        server.issueToken(
            "aad",
            klientId,
            DefaultOAuth2TokenCallback(
                issuerId = "aad",
                subject = klientId,
                audience = listOf(audience),
                claims = claims,
            )
        ).serialize()

    companion object {
        private const val NAV_IDENT = "Z999999"
        private const val ANNEN_GRUPPE = "00000000-0000-0000-0000-000000000002"
        private const val MELOSYS_WEB = "melosys-web-test"
        private const val MELOSYS_API = "melosys-api-test"
    }
}
