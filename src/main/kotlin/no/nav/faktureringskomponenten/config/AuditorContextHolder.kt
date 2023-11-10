package no.nav.faktureringskomponenten.config

import java.util.*


class AuditorContextHolder {
    companion object {
        private val currentAuditor = ThreadLocal<String>()

        fun setCurrentAuditor(auditor: String) {
            currentAuditor.set(auditor)
        }

        fun getCurrentAuditor(): Optional<String> {
            return Optional.ofNullable(currentAuditor.get())
        }

        fun clear() {
            currentAuditor.remove()
        }
    }
}
