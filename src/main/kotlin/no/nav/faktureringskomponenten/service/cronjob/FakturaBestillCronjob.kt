package no.nav.faktureringskomponenten.service.cronjob

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.faktureringskomponenten.service.FakturaService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FakturaBestillCronjob(
    @Autowired val fakturaService: FakturaService
) {
    private val log: Logger = LoggerFactory.getLogger(FakturaBestillCronjob::class.java)


    @Scheduled(cron = "*/10 * * * * *")
    @SchedulerLock(name = "bestill faktura", lockAtMostFor = "PT5M")
    fun bestillFaktura() {
        val alleFaktura = fakturaService.hentBestillingsklareFaktura()
        log.info("Kjører cronjob for å bestille ${alleFaktura.size} fakturaer")
        alleFaktura.forEach { faktura ->
            faktura.let {
                faktura.id?.let { fakturaId -> fakturaService.bestillFaktura(fakturaId) }
            }
        }
    }
}