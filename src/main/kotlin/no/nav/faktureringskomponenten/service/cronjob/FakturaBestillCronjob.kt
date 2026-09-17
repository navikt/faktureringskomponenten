package no.nav.faktureringskomponenten.service.cronjob

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

private val log = KotlinLogging.logger { }

@Component
class FakturaBestillCronjob(
    @Autowired val fakturaBestillingService: FakturaBestillingService
) {

    @Scheduled(cron = "\${cron.job.bestill-faktura}")
    @SchedulerLock(name = BESTILL_FAKTURA_LOCK_NAME, lockAtMostFor = LOCK_AT_MOST_FOR_ISO)
    fun bestillFaktura() {
        val alleFaktura = fakturaBestillingService.hentBestillingsklareFaktura()
        log.info("Kjører cronjob for å bestille ${alleFaktura.size} fakturaer")
        alleFaktura.forEach { faktura ->
            faktura.let {
                faktura.referanseNr.let { referanseNr -> fakturaBestillingService.bestillFaktura(referanseNr) }
            }
        }
    }

    companion object {
        /**
         * Låsnavnet deles med adminendepunktet som trigger bestilling on demand, slik at de to
         * aldri kjører samtidig. Endres navnet her, følger admintriggeren med.
         */
        const val BESTILL_FAKTURA_LOCK_NAME = "bestill faktura"
        const val LOCK_AT_MOST_FOR_ISO = "PT5M"
        val LOCK_AT_MOST_FOR: Duration = Duration.parse(LOCK_AT_MOST_FOR_ISO)
    }
}
