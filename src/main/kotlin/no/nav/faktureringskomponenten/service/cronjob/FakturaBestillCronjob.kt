package no.nav.faktureringskomponenten.service.cronjob

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class FakturaBestillCronjob(
    @Autowired val fakturaBestillingService: FakturaBestillingService
) {

    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "bestill faktura", lockAtMostFor = "PT5M")
    fun bestillFaktura() {
        val alleFaktura = fakturaBestillingService.hentBestillingsklareFaktura()
        log.info("Kjører cronjob for å bestille ${alleFaktura.size} fakturaer")
        alleFaktura.forEach { faktura ->
            faktura.let {
                faktura.referanseNr.let { referanseNr -> fakturaBestillingService.bestillFaktura(referanseNr) }
            }
        }
    }
}
