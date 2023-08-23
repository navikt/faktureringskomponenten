package no.nav.faktureringskomponenten.config

import org.slf4j.MDC
import java.util.*

object MDCOperations {
    const val CORRELATION_ID = "correlation-id"
    const val X_CORRELATION_ID = "X-Correlation-ID"
    fun putToMDC(key: String?, value: String?) {
        MDC.put(key, value)
    }

    fun remove(key: String?) {
        MDC.remove(key)
    }

    val correlationId: String
        get() {
            var correlationId = MDC.get(CORRELATION_ID)
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString()
            }
            return correlationId
        }
}