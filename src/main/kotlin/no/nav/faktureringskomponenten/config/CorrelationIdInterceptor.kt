package no.nav.faktureringskomponenten.config


import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.faktureringskomponenten.config.MDCOperations.CORRELATION_ID
import no.nav.faktureringskomponenten.config.MDCOperations.X_CORRELATION_ID
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import java.util.*


class CorrelationIdInterceptor : HandlerInterceptor {
    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val correlationId = getCorrelationId(request)
        MDCOperations.putToMDC(CORRELATION_ID, correlationId)
        LOGGER.debug("Set MDC values")
        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        MDCOperations.remove(CORRELATION_ID)
        LOGGER.debug("Cleared MDC values")
    }

    private fun getCorrelationId(request: HttpServletRequest): String {
        return if (isMissingCorrelationId(request.getHeader(X_CORRELATION_ID))) {
            UUID.randomUUID().toString()
        } else request.getHeader(X_CORRELATION_ID)
    }

    private fun isMissingCorrelationId(correlationId: String?): Boolean {
        if (correlationId != null) {
            return Objects.isNull(correlationId) || correlationId.isBlank()
        }
        return true
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CorrelationIdInterceptor::class.java)
    }
}
