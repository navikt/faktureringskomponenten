package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

private val log = KotlinLogging.logger { }

/**
 * Adminfunksjonalitet for bestilling av fakturaer. Holdes adskilt fra [FakturaBestillingService],
 * som er den ordinære produksjonsflyten.
 */
@Service
class AdminBestillingService(
    private val fakturaBestillingService: FakturaBestillingService,
    lockProvider: LockProvider,
) {

    private val lockingTaskExecutor: LockingTaskExecutor = DefaultLockingTaskExecutor(lockProvider)

    /**
     * Bestiller alle bestillingsklare fakturaer med én gang, uten å vente på cronjobben.
     *
     * Tar samme ShedLock-lås som [no.nav.faktureringskomponenten.service.cronjob.FakturaBestillCronjob],
     * slik at manuell trigging aldri kjører samtidig som den planlagte jobben.
     *
     * @return referansenumrene til fakturaene som ble bestilt, eller null dersom låsen var opptatt
     */
    fun bestillBestillingsklareFakturaer(bestillingsDato: LocalDate = LocalDate.now()): List<String>? {
        val resultat = lockingTaskExecutor.executeWithLock(
            LockingTaskExecutor.TaskWithResult { bestill(bestillingsDato) },
            LockConfiguration(Instant.now(), BESTILL_FAKTURA_LOCK_NAME, LOCK_AT_MOST_FOR, Duration.ZERO)
        )

        if (!resultat.wasExecuted()) {
            log.info("Bestilling av fakturaer kjører allerede, admin-trigging hoppet over")
            return null
        }

        return resultat.result.orEmpty()
    }

    private fun bestill(bestillingsDato: LocalDate): List<String> {
        val bestillingsklareFaktura = fakturaBestillingService.hentBestillingsklareFaktura(bestillingsDato)
        log.info("Admin bestiller ${bestillingsklareFaktura.size} fakturaer med bestillingsdato til og med $bestillingsDato")
        return bestillingsklareFaktura.map { faktura ->
            fakturaBestillingService.bestillFaktura(faktura.referanseNr)
            faktura.referanseNr
        }
    }

    companion object {
        const val BESTILL_FAKTURA_LOCK_NAME = "bestill faktura"
        private val LOCK_AT_MOST_FOR: Duration = Duration.ofMinutes(5)
    }
}
