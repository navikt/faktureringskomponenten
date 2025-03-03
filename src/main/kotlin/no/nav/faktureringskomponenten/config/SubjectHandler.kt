package no.nav.faktureringskomponenten.config

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

class SubjectHandler private constructor(private val contextHolder: SpringTokenValidationContextHolder) {

    fun getTokenUsername(): String? {
        // Først prøv å hente NAVident
        val navIdent = getClaimFromToken<String>("NAVident")
        if (!navIdent.isNullOrBlank()) {
            return navIdent
        }

        // Hvis NAVident ikke er tilgjengelig, returneres det menneskelig lesbare maskinnavnet (azp_name).
        return getClaimFromToken<String>("azp_name")
    }

    private inline fun <reified T> getClaimFromToken(claim: String): T? = getOptionalContext()
        ?.getJwtToken(ISSUER)
        ?.jwtTokenClaims
        ?.get(claim) as? T

    private fun getOptionalContext(): TokenValidationContext? = try {
        contextHolder.getTokenValidationContext()
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val ISSUER = "aad"

        private object SubjectHandlerHolder {
            val INSTANCE: SubjectHandler = SubjectHandler(SpringTokenValidationContextHolder())
        }

        fun getInstance(): SubjectHandler {
            return SubjectHandlerHolder.INSTANCE
        }
    }
}
