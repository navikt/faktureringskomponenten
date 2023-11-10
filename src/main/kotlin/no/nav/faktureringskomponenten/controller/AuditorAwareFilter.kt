package no.nav.faktureringskomponenten.controller

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import no.nav.faktureringskomponenten.config.AuditorContextHolder
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class AuditorAwareFilter : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest

        if (httpRequest.method.equals("POST", false)) {
            val auditor = httpRequest.getHeader(NAV_USER_ID)
            if (auditor == null) {
                val httpResponse = response as HttpServletResponse
                httpResponse.sendError(HttpStatus.BAD_REQUEST.value(), "NAV ident må oppgis for sporing")
                return
            }

            log.debug { "Request med $NAV_USER_ID: $auditor" }
            AuditorContextHolder.setCurrentAuditor(auditor)
        }


        try {
            chain.doFilter(request, response)
        } finally {
            AuditorContextHolder.clear()
            log.debug { "Auditor cleared" }
        }
    }

    companion object {
        const val NAV_USER_ID = "Nav-User-Id"
    }
}