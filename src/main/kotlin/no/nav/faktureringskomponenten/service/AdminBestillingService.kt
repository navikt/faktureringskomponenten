package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor
import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import no.nav.faktureringskomponenten.service.cronjob.FakturaBestillCronjob
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
    private val adminService: AdminService,
    lockProvider: LockProvider,
) {

    private val lockingTaskExecutor: LockingTaskExecutor = DefaultLockingTaskExecutor(lockProvider)

    /**
     * Bestiller alle bestillingsklare fakturaer med én gang, uten å vente på cronjobben.
     *
     * Tar samme ShedLock-lås som [FakturaBestillCronjob], slik at manuell trigging aldri kjører
     * samtidig som den planlagte jobben. Merk at låsen, som for cronjobben, slippes etter
     * [FakturaBestillCronjob.LOCK_AT_MOST_FOR] selv om kjøringen fortsatt pågår. Tar bestillingen
     * lengre tid enn dette, kan cronjobben hente de samme fakturaene.
     *
     * Feiler bestillingen av én faktura, fortsetter resten, og fakturaen rapporteres som feilet.
     *
     * @return resultatet av kjøringen, eller null dersom låsen var opptatt
     */
    fun bestillBestillingsklareFakturaer(bestillingsDato: LocalDate = LocalDate.now()): Bestillingsresultat? {
        val resultat = medBestillingslås { bestill(bestillingsDato) }
        if (resultat == null) {
            log.info("Bestilling av fakturaer kjører allerede, admin-trigging hoppet over")
        }
        return resultat
    }

    /**
     * Endrer bestillingsdato på én faktura uten å bestille den. Cronjobben bestiller fakturaen
     * når datoen er nådd.
     *
     * Tar samme ShedLock-lås som [FakturaBestillCronjob], slik at endringen aldri kjører samtidig
     * som cronjobben bestiller den samme fakturaen.
     *
     * @return resultatet av endringen, eller null dersom låsen var opptatt
     */
    fun endreBestillingsdato(fakturaReferanse: String, bestillingsDato: LocalDate): EndreBestillingsdatoResultat? {
        val resultat = medBestillingslås { adminService.endreBestillingsdato(fakturaReferanse, bestillingsDato) }
        if (resultat == null) {
            log.info("Bestilling av fakturaer kjører allerede, endring av bestillingsdato på $fakturaReferanse hoppet over")
        }
        return resultat
    }

    /**
     * Setter bestillingsdato til i dag og bestiller én faktura til OEBS med en gang.
     *
     * Tar samme ShedLock-lås som [FakturaBestillCronjob], slik at fakturaen aldri bestilles
     * samtidig av cronjobben.
     *
     * @return resultatet av bestillingen, eller null dersom låsen var opptatt
     */
    fun bestillUmiddelbart(fakturaReferanse: String): BestillFakturaResultat? {
        val resultat = medBestillingslås { adminService.bestillUmiddelbart(fakturaReferanse) }
        if (resultat == null) {
            log.info("Bestilling av fakturaer kjører allerede, bestilling av $fakturaReferanse hoppet over")
        }
        return resultat
    }

    private fun <T> medBestillingslås(oppgave: () -> T): T? {
        val resultat = lockingTaskExecutor.executeWithLock(
            LockingTaskExecutor.TaskWithResult { oppgave() },
            LockConfiguration(
                Instant.now(),
                FakturaBestillCronjob.BESTILL_FAKTURA_LOCK_NAME,
                FakturaBestillCronjob.LOCK_AT_MOST_FOR,
                Duration.ZERO
            )
        )
        return if (resultat.wasExecuted()) resultat.result else null
    }

    private fun bestill(bestillingsDato: LocalDate): Bestillingsresultat {
        val bestillingsklareFakturaer = fakturaBestillingService.hentBestillingsklareFaktura(bestillingsDato)
        log.info("Admin bestiller ${bestillingsklareFakturaer.size} fakturaer med bestillingsdato til og med $bestillingsDato")

        val bestilte = mutableListOf<String>()
        val feilede = mutableListOf<FeiletBestilling>()

        bestillingsklareFakturaer.forEach { faktura ->
            try {
                if (fakturaBestillingService.bestillFaktura(faktura.referanseNr, bestillingsDato)) {
                    bestilte.add(faktura.referanseNr)
                }
            } catch (e: Exception) {
                log.error(e) { "Klarte ikke å bestille faktura med referanse nr ${faktura.referanseNr}" }
                feilede.add(FeiletBestilling(faktura.referanseNr, e.message ?: e::class.simpleName.orEmpty()))
            }
        }

        return Bestillingsresultat(bestilte, feilede)
    }
}

data class Bestillingsresultat(
    val bestilte: List<String>,
    val feilede: List<FeiletBestilling>
)

data class FeiletBestilling(
    val fakturaReferanse: String,
    val feilmelding: String
)
