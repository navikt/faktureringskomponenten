package no.nav.faktureringskomponenten.service.cronjob

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
private val log = KotlinLogging.logger { }

@Component
class FakturaBestillKrediteringCronjob(
    @Autowired val fakturaBestillingService: FakturaBestillingService
) {

    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "krediter faktura", lockAtMostFor = "PT5M")
    fun krediterFaktura() {
        val alleFaktura = fakturaBestillingService.hentKrediteringsklareFaktura()
        log.info("Kjører cronjob for å bestille kreditering for ${alleFaktura.size} fakturaer")
        alleFaktura.forEach { faktura ->
            faktura.let {
                faktura.referanseNr.let { referanseNr -> fakturaBestillingService.krediterFaktura(referanseNr) }
            }
        }
    }
}