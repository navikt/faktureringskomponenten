package no.nav.faktureringskomponenten.security

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.web.context.request.RequestContextHolder


class SubjectHandler(private val contextHolder: SpringTokenValidationContextHolder) {

    private fun hasValidToken(): Boolean {
        contextHolder.tokenValidationContext
        return RequestContextHolder.getRequestAttributes() != null && context().hasTokenFor(AAD)
    }

    private fun jwtToken(): JwtToken {
        return context().getJwtToken(AAD)
    }

    private fun context(): TokenValidationContext {
        return contextHolder.tokenValidationContext
    }

    companion object {
        const val AAD = "aad"
        val SUBJECT_HANDLER = SubjectHandler(SpringTokenValidationContextHolder())

        fun getInstance(): SubjectHandler {
            return SUBJECT_HANDLER
        }
    }
}
