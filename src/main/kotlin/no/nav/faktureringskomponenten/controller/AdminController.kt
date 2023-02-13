package no.nav.faktureringskomponenten.controller

import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaMottattConsumer
import no.nav.security.token.support.core.api.Protected
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Protected
@Validated
@RestController
@RequestMapping("/admin")
class AdminController(
    val fakturaMottakFeilRepository: FakturaMottakFeilRepository,
    val fakturaMottattConsumer: FakturaMottattConsumer
) {
    @GetMapping("/faktura/mottak/feil")
    fun hentFakturaMottakFeil(): ResponseEntity<Map<Long?, List<FakturaMottakFeil>>> {
        val groupBy: Map<Long?, List<FakturaMottakFeil>> =
            fakturaMottakFeilRepository.findAll().groupBy { it.kafkaOffset }
        return ResponseEntity.ok(groupBy.toSortedMap(compareBy { it }))
    }

    @PostMapping("/faktura/mottak/consumer/stop")
    fun stoppKafkaConsumer(): ResponseEntity<String> {
        log.info("Stopper faktura mottat consumer")
        fakturaMottattConsumer.stop()
        return ResponseEntity.ok("Stoppet faktura mottat consumer")
    }

    @PostMapping("/faktura/mottak/consumer/start")
    fun startKafkaConsumer(): ResponseEntity<String> {
        log.info("Starter faktura mottak consumer")
        fakturaMottattConsumer.start()
        return ResponseEntity.ok("Startet faktura mottak consumer")
    }

    @PostMapping("/faktura/mottak/consumer/seek/{offset}")
    fun settKafkaOffset(@PathVariable offset: Long): ResponseEntity<String> {
        log.info("setter offset for faktura mottak consumer til: $offset")
        fakturaMottattConsumer.settSpesifiktOffsetPåConsumer(offset)
        return ResponseEntity.ok("satt offset for faktura mottak consumer")
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AdminController::class.java)
    }
}
