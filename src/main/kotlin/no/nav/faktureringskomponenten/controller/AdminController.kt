package no.nav.faktureringskomponenten.controller

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.faktureringskomponenten.service.EksternFakturaStatusService
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import no.nav.faktureringskomponenten.service.FakturaService
import no.nav.faktureringskomponenten.service.integration.kafka.EksternFakturaStatusConsumer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Protected
@Validated
@RestController
@RequestMapping("/admin")
class AdminController(
    val fakturaMottakFeilRepository: FakturaMottakFeilRepository,
    val eksternFakturaStatusConsumer: EksternFakturaStatusConsumer,
    val eksternFakturaStatusService: EksternFakturaStatusService,
    val fakturaService: FakturaService,
    val fakturaBestillingService: FakturaBestillingService
) {

    @Value("\${NAIS_CLUSTER_NAME}")
    private lateinit var naisClusterName: String

    private val naisClusterNameDev = "dev-gcp"

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

    @PostMapping("/faktura/{fakturaReferanse}/ombestill")
    fun ombestillFaktura(@PathVariable fakturaReferanse: String): ResponseEntity<String> {
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
        fakturaService.oppdaterFakturaStatus(fakturaReferanse, FakturaStatus.OPPRETTET)

        fakturaBestillingService.bestillFaktura(fakturaReferanse)
        return ResponseEntity.ok("Feilet faktura med referanse nr $fakturaReferanse bestilles på nytt")
    }

    /**
     * Simulerer at faktura ikke er betalt innen forfall. Endepunktet er KUN tilgjengelig i testmiljø.
     */
    @PostMapping("/faktura/{fakturaReferanse}/manglende-innbetaling")
    fun simulerManglendeInnbetaling(@PathVariable fakturaReferanse: String): ResponseEntity<String> {
        if (naisClusterName != naisClusterNameDev) {
            log.warn("Endepunktet er kun tilgjengelig i testmiljø")
            return ResponseEntity.status(HttpStatusCode.valueOf(403))
                .body("Endepunktet er kun tilgjengelig i testmiljø")
        }

        val faktura = fakturaService.hentFaktura(fakturaReferanse) ?:
            return ResponseEntity.status(HttpStatusCode.valueOf(404))
                .body("Finner ikke faktura med referanse nr $fakturaReferanse")

        if (faktura.status != FakturaStatus.BESTILT) {
            log.info("Faktura med referanse nr $fakturaReferanse må ha status BESTILT")
            return ResponseEntity.status(HttpStatusCode.valueOf(400))
                .body("Faktura med referanse nr $fakturaReferanse må ha status BESTILT")
        }

        val simulertEksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = fakturaReferanse,
            fakturaNummer = "SIMULERT_MANGLENDE_INNBETALING",
            fakturaBelop = faktura.totalbeløp(),
            ubetaltBelop = BigDecimal(0),
            status = FakturaStatus.MANGLENDE_INNBETALING,
            dato = LocalDate.now(),
            feilmelding = "Simulerer manglende innbetaling"
        )

        eksternFakturaStatusService.lagreEksternFakturaStatusMelding(simulertEksternFakturaStatusDto)

        log.info("Simulerer manglende innbetaling for faktura med referanse nr $fakturaReferanse")
        return ResponseEntity.ok("Simulerer manglende innbetaling for faktura med referanse nr $fakturaReferanse")
    }
}
