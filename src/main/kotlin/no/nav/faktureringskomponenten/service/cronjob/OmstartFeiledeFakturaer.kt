package no.nav.faktureringskomponenten.service.cronjob

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.faktureringskomponenten.domain.models.ErrorTypes
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class OmstartFeiledeFakturaer(
    @Autowired val fakturaBestillingService: FakturaBestillingService,
    @Autowired val fakturaMottakFeilRepository: FakturaMottakFeilRepository
) {

    @Scheduled(cron = "0 */1 * * * *")
    @SchedulerLock(name = "bestill feilede fakturaer på nytt", lockAtMostFor = "PT5M")
    fun bestillFaktura() {
        val feiledeFakturaer = fakturaMottakFeilRepository.findAllByErrorTypeOrderByCreatedAt(ErrorTypes.MANGLENDE_OPPLYSNINGER)

        log.info("Kjører cronjob for å bestille ${feiledeFakturaer.size} feilede fakturaer")
        feiledeFakturaer.forEach { faktura ->
            faktura.let {
                faktura.fakturaReferanseNr.let { referanseNr ->
                    if (referanseNr != null) {
                        try {
                            fakturaBestillingService.bestillFaktura(referanseNr)
                        } catch (e: Exception) {
                            log.error("Feil ved bestilling av faktura med referanseNr: $referanseNr", e)
                        } finally {
                            fakturaMottakFeilRepository.delete(faktura)
                        }
                    }
                }
            }
        }
    }
}