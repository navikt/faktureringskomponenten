package no.nav.faktureringskomponenten.controller

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import no.nav.faktureringskomponenten.service.FakturaService
import no.nav.faktureringskomponenten.service.integration.kafka.EksternFakturaStatusConsumer
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Protected
@Validated
@RestController
@RequestMapping("/admin")
class AdminController(
    val fakturaMottakFeilRepository: FakturaMottakFeilRepository,
    val eksternFakturaStatusConsumer: EksternFakturaStatusConsumer,
    val fakturaService: FakturaService,
    val fakturaBestillingService: FakturaBestillingService
) {
    @GetMapping("/faktura/mottak/feil")
    fun hentFakturaMottakFeil(): ResponseEntity<Map<Long?, List<FakturaMottakFeil>>> {
        val groupBy: Map<Long?, List<FakturaMottakFeil>> =
            fakturaMottakFeilRepository.findAll().groupBy { it.kafkaOffset ?: -1 }
        return ResponseEntity.ok(groupBy.toSortedMap(compareBy { it }))
    }

    @PostMapping("/faktura/mottak/consumer/stop")
    fun stoppKafkaConsumer(): ResponseEntity<String> {
        log.info("Stopper faktura mottat consumer")
        eksternFakturaStatusConsumer.stop()
        return ResponseEntity.ok("Stoppet faktura mottat consumer")
    }

    @PostMapping("/faktura/mottak/consumer/start")
    fun startKafkaConsumer(): ResponseEntity<String> {
        log.info("Starter faktura mottak consumer")
        eksternFakturaStatusConsumer.start()
        return ResponseEntity.ok("Startet faktura mottak consumer")
    }

    @PostMapping("/faktura/mottak/consumer/seek/{offset}")
    fun settKafkaOffset(@PathVariable offset: Long): ResponseEntity<String> {
        log.info("setter offset for faktura mottak consumer til: $offset")
        eksternFakturaStatusConsumer.settSpesifiktOffsetPåConsumer(offset)
        return ResponseEntity.ok("satt offset for faktura mottak consumer")
    }

    @PostMapping("/faktura/rebestill/{fakturaReferanse}")
    fun rebestillFaktura(@PathVariable fakturaReferanse: String): ResponseEntity<String> {
        log.info("Sender ny melding til OEBS om bestilling av faktura med referanse nr $fakturaReferanse")
        val faktura = fakturaService.hentFaktura(fakturaReferanse)
        if (faktura == null) {
            log.info("Finner ikke faktura med referanse nr $fakturaReferanse")
            return ResponseEntity.status(HttpStatusCode.valueOf(404))
                .body("Finner ikke faktura med referanse nr $fakturaReferanse")
        }
        if (faktura.status != FakturaStatus.FEIL) {
            log.info("Faktura med referanse nr $fakturaReferanse er ikke i feil status")
            return ResponseEntity.status(HttpStatusCode.valueOf(400))
                .body("Faktura med referanse nr $fakturaReferanse er ikke i feil status")
        }
        fakturaBestillingService.bestillFaktura(fakturaReferanse)
        return ResponseEntity.ok("Feilet faktura med referanse nr $fakturaReferanse bestilles på nytt")
    }
}
