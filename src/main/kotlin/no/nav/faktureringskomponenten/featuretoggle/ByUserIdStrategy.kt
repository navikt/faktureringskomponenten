package no.nav.faktureringskomponenten.featuretoggle

import io.getunleash.strategy.Strategy
import mu.KotlinLogging
import no.nav.faktureringskomponenten.config.AuditorContextHolder
import java.util.*

internal class ByUserIdStrategy : Strategy {
    private val log = KotlinLogging.logger { }
    private val uniqueLogMessages = Collections.synchronizedSet(HashSet<String>())

    override fun getName(): String = "byUserId"

    override fun isEnabled(parameters: Map<String, String>): Boolean {
        val userIDs = parameters["user"]
        if (userIDs.isNullOrBlank()) return false

        val loggedInUserID = getLoggedInUserID()
        if (loggedInUserID.isNullOrBlank()) {
            logOnlyFirstMessage(
                "Unleash byUserId Strategy brukes uten innlogget saksbehandler, blir kalt fra:\n${calledFrom()}"
            )
            return false
        }

        return userIDs.split(",").contains(loggedInUserID)
    }

    private fun logOnlyFirstMessage(msg: String) {
        if (uniqueLogMessages.add(msg)) {
            log.warn(msg)
        }
    }

    private fun calledFrom(): String =
        Thread.currentThread().stackTrace.let { stackTraceElements ->
            val element = stackTraceElements.find {
                it.toString().contains(STACK_TRACE_LINE_BEFORE_UNLEASH_IS_ENABLED)
            } ?: return@let "Fant ikke unleash bruk i stacktrace\n" + stackTraceElements.joinToString("\n")
            val indexToLineAfterUnleashIsEnabledCall = stackTraceElements.indexOf(element)
            stackTraceElements[indexToLineAfterUnleashIsEnabledCall + 1].toString()
        }

    /**
     * Henter innlogget bruker-ID, med prioritering:
     * 1. Fra token (SubjectHandler) - mest sikker, basert på autentisert token
     * 2. Fra AuditorContextHolder - fallback når token-context ikke er tilgjengelig
     *
     * SubjectHandler bruker token-basert autentisering, mens AuditorContextHolder
     * kan settes via Nav-User-Id header (mindre sikkert) men er nyttig som fallback.
     */
    private fun getLoggedInUserID(): String? =
        AuditorContextHolder.getCurrentAuditor().orElse(null)

    companion object {
        const val STACK_TRACE_LINE_BEFORE_UNLEASH_IS_ENABLED =
            "io.getunleash.DefaultUnleash.isEnabled(DefaultUnleash.java:93"
    }
}
