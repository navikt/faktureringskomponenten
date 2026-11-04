package no.nav.faktureringskomponenten.config

import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.EmbeddedKafkaBase
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ActiveProfiles("itest", "embeded-kafka")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
class TomcatConfigIT(
    @param:Autowired private val webClient: WebTestClient,
    @param:Autowired fakturaserieRepository: FakturaserieRepository,
) : EmbeddedKafkaBase(fakturaserieRepository) {

    @Test
    fun `request med mange språk i Accept-Language avvises ikke`() {
        val mangeSpråk = listOf("nb", "nn", "no", "en", "en-US", "en-GB", "sv", "da", "de", "fr", "es", "it")
            .joinToString(",")

        webClient.get()
            .uri("/internal/health")
            .header(HttpHeaders.ACCEPT_LANGUAGE, mangeSpråk)
            .exchange()
            .expectStatus().isOk
    }
}
