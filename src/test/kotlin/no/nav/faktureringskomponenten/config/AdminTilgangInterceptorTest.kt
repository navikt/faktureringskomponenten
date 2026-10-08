package no.nav.faktureringskomponenten.config

import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.PlainJWT
import io.kotest.matchers.shouldBe
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtToken
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

/**
 * Tom konfigurasjon skal ikke stoppe oppstart, men gi 403 på adminrutene.
 * Dekkes her fordi integrasjonstestene kjører med utfylt konfigurasjon.
 */
class AdminTilgangInterceptorTest {

    @Test
    fun `tom Console-klient-ID avviser kall med tom azp`() {
        val respons = kall(mapOf("azp" to "", "idtyp" to "app"), consoleKlientId = "")

        respons.status shouldBe 403
        respons.contentAsString shouldBe AdminTilgangInterceptor.UKJENT_KLIENT
    }

    @Test
    fun `tom Console-klient-ID avviser kall uten azp`() {
        val respons = kall(mapOf("idtyp" to "app"), consoleKlientId = "")

        respons.status shouldBe 403
        respons.contentAsString shouldBe AdminTilgangInterceptor.UKJENT_KLIENT
    }

    @Test
    fun `tom driftsgruppe avviser personkall med tom gruppe`() {
        val respons = kall(mapOf("azp" to CONSOLE, "groups" to listOf("")), driftsgruppeId = "")

        respons.status shouldBe 403
        respons.contentAsString shouldBe AdminTilgangInterceptor.MANGLER_DRIFTSGRUPPE
    }

    @Test
    fun `tom driftsgruppe stopper ikke maskinkall fra Console`() {
        val respons = kall(mapOf("azp" to CONSOLE, "idtyp" to "app"), driftsgruppeId = "")

        respons.status shouldBe 200
    }

    private fun kall(
        claims: Map<String, Any>,
        driftsgruppeId: String = DRIFTSGRUPPE,
        consoleKlientId: String = CONSOLE,
    ): MockHttpServletResponse {
        val interceptor = AdminTilgangInterceptor(holderMed(claims), driftsgruppeId, consoleKlientId)
        val response = MockHttpServletResponse()
        interceptor.preHandle(MockHttpServletRequest("GET", "/admin/faktura/mottak/feil"), response, Any())
        return response
    }

    // Tokenet er allerede validert når interceptoren kjører, så et usignert token holder her
    private fun holderMed(claims: Map<String, Any>) = object : TokenValidationContextHolder {
        override fun getTokenValidationContext() =
            TokenValidationContext(mapOf("aad" to JwtToken(PlainJWT(claimsSet(claims)).serialize())))

        override fun setTokenValidationContext(tokenValidationContext: TokenValidationContext?) = Unit
    }

    private fun claimsSet(claims: Map<String, Any>): JWTClaimsSet =
        JWTClaimsSet.Builder().apply { claims.forEach { (navn, verdi) -> claim(navn, verdi) } }.build()

    companion object {
        private const val CONSOLE = "melosys-console-test"
        private const val DRIFTSGRUPPE = "driftsgruppe-test"
    }
}
