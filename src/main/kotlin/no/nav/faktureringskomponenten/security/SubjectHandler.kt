package no.nav.faktureringskomponenten.security

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.web.context.request.RequestContextHolder

class SubjectHandler(private val contextHolder: SpringTokenValidationContextHolder) {

    val userID: String?
        get() = if (hasValidToken()) azureActiveDirectoryToken().getJwtTokenClaims().get(JWT_TOKEN_CLAIM_NAVIDENT)
            .toString() else null

    private fun hasValidToken(): Boolean {
        contextHolder.tokenValidationContext
        return RequestContextHolder.getRequestAttributes() != null && context().hasTokenFor(azureActiveDirectory)
    }

    private fun azureActiveDirectoryToken(): JwtToken {
        return context().getJwtToken(azureActiveDirectory)
    }

    private fun context(): TokenValidationContext {
        return contextHolder.tokenValidationContext
    }

    companion object {
        const val azureActiveDirectory = "aad"
        const val JWT_TOKEN_CLAIM_NAVIDENT = "NAVident"
        val SUBJECT_HANDLER = SubjectHandler(SpringTokenValidationContextHolder())

        fun getInstance(): SubjectHandler {
            return SUBJECT_HANDLER
        }
    }
}
