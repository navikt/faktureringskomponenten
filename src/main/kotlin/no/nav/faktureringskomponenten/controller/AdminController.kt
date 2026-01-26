package no.nav.faktureringskomponenten.controller

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import mu.KotlinLogging
import no.nav.faktureringskomponenten.controller.dto.FakturaAdminDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.dto.NyFakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.dto.toFakturaAdminDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.validators.OrganisasjonsnummerValidator
import no.nav.faktureringskomponenten.domain.models.AvstemmingCsvRad
import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import no.nav.faktureringskomponenten.service.FakturamottakerDto
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.service.*
import no.nav.faktureringskomponenten.service.integration.kafka.EksternFakturaStatusConsumer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

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
    val fakturaBestillingService: FakturaBestillingService,
    val adminService: AdminService,
    val faktureringService: FakturaserieService,
    val fakturaRepository: FakturaRepository
) {

    @Value("\${NAIS_CLUSTER_NAME}")
    private lateinit var naisClusterName: String

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
    fun ombestillFaktura(
        @PathVariable fakturaReferanse: String,
        @RequestParam(required = false) fakturaMottaker: String?
    ): ResponseEntity<String> {
        log.info("Sender ny melding til OEBS om bestilling av faktura med referanse nr $fakturaReferanse")
        val faktura = fakturaService.hentFaktura(fakturaReferanse)
        if (faktura == null) {
            log.info("Finner ikke faktura med referanse nr $fakturaReferanse")
            return ResponseEntity.status(404)
                .body("Finner ikke faktura med referanse nr $fakturaReferanse")
        }
        if (faktura.status != FakturaStatus.FEIL) {
            log.info("Faktura med referanse nr $fakturaReferanse er ikke i feil status")
            return ResponseEntity.status(400)
                .body("Faktura med referanse nr $fakturaReferanse er ikke i feil status")
        }

        if (fakturaMottaker != null && !OrganisasjonsnummerValidator.erGyldig(fakturaMottaker)) {
            log.info("Ugyldig organisasjonsnummer: $fakturaMottaker")
            return ResponseEntity.status(400)
                .body("Ugyldig organisasjonsnummer: $fakturaMottaker")
        }

        fakturaService.oppdaterFakturaStatus(fakturaReferanse, FakturaStatus.OPPRETTET)

        if (fakturaMottaker != null) {
            val fakturaserie = faktura.fakturaserie
            if (fakturaserie != null) {
                log.info("Oppdaterer fakturaMottaker til $fakturaMottaker for fakturaserie ${fakturaserie.referanse}")
                faktureringService.endreFakturaMottaker(
                    fakturaserie.referanse,
                    FakturamottakerDto(Fullmektig(organisasjonsnummer = fakturaMottaker))
                )
            }
        }

        fakturaBestillingService.bestillFaktura(fakturaReferanse)
        return ResponseEntity.ok("Feilet faktura med referanse nr $fakturaReferanse bestilles på nytt")
    }

    @PostMapping("/faktura/{fakturaReferanse}/krediter")
    fun krediterFaktura(@PathVariable fakturaReferanse: String): FakturaserieResponseDto {
        log.info("Krediterer faktura med referanse nr $fakturaReferanse")

        return adminService.krediterFaktura(fakturaReferanse).tilFakturaserieResponseDto()
    }

    /**
     * Simulerer at faktura ikke er betalt innen forfall. Endepunktet er KUN tilgjengelig i testmiljø.
     */
    @PostMapping("/faktura/{fakturaReferanse}/manglende-innbetaling")
    fun simulerManglendeInnbetaling(
        @PathVariable fakturaReferanse: String,
        @RequestParam(required = false, defaultValue = "0") betaltBelop: BigDecimal
    ): ResponseEntity<String> {
        if (naisClusterName != NAIS_CLUSTER_NAME_DEV) {
            log.warn("Endepunktet er kun tilgjengelig i testmiljø")
            return ResponseEntity.status(403)
                .body("Endepunktet er kun tilgjengelig i testmiljø")
        }

        val faktura = fakturaService.hentFaktura(fakturaReferanse) ?: return ResponseEntity.status(404)
            .body("Finner ikke faktura med referanse nr $fakturaReferanse")

        if (faktura.status != FakturaStatus.BESTILT) {
            log.info("Faktura med referanse nr $fakturaReferanse må ha status BESTILT")
            return ResponseEntity.status(400)
                .body("Faktura med referanse nr $fakturaReferanse må ha status BESTILT")
        }

        if (faktura.totalbeløp() <= betaltBelop) {
            log.info("Faktura med referanse nr $fakturaReferanse må ha betalt beløp mindre enn totalbeløp")
            return ResponseEntity.status(400)
                .body("Faktura med referanse nr $fakturaReferanse må ha betalt beløp mindre enn totalbeløp")
        }

        val simulertEksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = fakturaReferanse,
            fakturaNummer = (99990000..99999999).random().toString(),
            fakturaBelop = faktura.totalbeløp(),
            ubetaltBelop = faktura.totalbeløp() - betaltBelop,
            status = FakturaStatus.MANGLENDE_INNBETALING,
            dato = LocalDate.now(),
            feilmelding = "Simulert manglende innbetaling"
        )

        eksternFakturaStatusService.håndterEksternFakturaStatusMelding(simulertEksternFakturaStatusDto)

        log.info("Simulert manglende innbetaling for faktura med referanse nr $fakturaReferanse")
        return ResponseEntity.ok("Simulert manglende innbetaling for faktura med referanse nr $fakturaReferanse")
    }

    @PostMapping("/faktura/{fakturaReferanse}/manglende-innbetaling-prod")
    fun simulerManglendeInnbetalingProd(
        @PathVariable fakturaReferanse: String,
        @RequestBody manglendeInnbetalingSimuleringDto: ManglendeInnbetalingSimuleringDto
    ): ResponseEntity<String> {
        val faktura = fakturaService.hentFaktura(fakturaReferanse) ?: return ResponseEntity.status(404)
            .body("Finner ikke faktura med referanse nr $fakturaReferanse")

        if (faktura.status != FakturaStatus.BESTILT) {
            log.info("Faktura med referanse nr $fakturaReferanse må ha status BESTILT")
            return ResponseEntity.status(400)
                .body("Faktura med referanse nr $fakturaReferanse må ha status BESTILT")
        }

        if (faktura.totalbeløp() <= manglendeInnbetalingSimuleringDto.betaltBelop) {
            log.info("Faktura med referanse nr $fakturaReferanse må ha betalt beløp mindre enn totalbeløp")
            return ResponseEntity.status(400)
                .body("Faktura med referanse nr $fakturaReferanse må ha betalt beløp mindre enn totalbeløp")
        }

        val simulertEksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = fakturaReferanse,
            fakturaNummer = manglendeInnbetalingSimuleringDto.fakturaNummer,
            fakturaBelop = faktura.totalbeløp(),
            ubetaltBelop = faktura.totalbeløp() - manglendeInnbetalingSimuleringDto.betaltBelop,
            status = FakturaStatus.MANGLENDE_INNBETALING,
            dato = LocalDate.now(),
            feilmelding = "Simulert manglende innbetaling"
        )

        eksternFakturaStatusService.håndterEksternFakturaStatusMelding(simulertEksternFakturaStatusDto)

        log.info("Simulert manglende innbetaling for faktura med referanse nr $fakturaReferanse")
        return ResponseEntity.ok("Simulert manglende innbetaling for faktura med referanse nr $fakturaReferanse")
    }

    /**
     * Endrer status på faktura. Endepunktet er KUN tilgjengelig i testmiljø.
     */
    @PostMapping("/faktura/{fakturaReferanse}/status")
    fun endreFakturastatus(
        @PathVariable fakturaReferanse: String,
        @RequestParam(required = false, defaultValue = "BESTILT") status: FakturaStatus
    ): ResponseEntity<String> {
        if (naisClusterName != NAIS_CLUSTER_NAME_DEV) {
            log.warn("Endepunktet er kun tilgjengelig i testmiljø")
            return ResponseEntity.status(403)
                .body("Endepunktet er kun tilgjengelig i testmiljø")
        }

        val faktura = fakturaService.hentFaktura(fakturaReferanse) ?: return ResponseEntity.status(404)
            .body("Finner ikke faktura med referanse nr $fakturaReferanse")

        if (faktura.status == status) {
            log.info("Faktura med referanse nr $fakturaReferanse har allerede statusen $status")
            return ResponseEntity.status(400)
                .body("Faktura med referanse nr $fakturaReferanse har allerede statusen $status")
        }

        val originalStatus = faktura.status

        fakturaService.oppdaterFakturaStatus(fakturaReferanse, status)

        log.info("Status på faktura $fakturaReferanse har blitt oppdatert fra fra $originalStatus til $status")
        return ResponseEntity.ok("Status på faktura $fakturaReferanse har blitt oppdatert fra fra $originalStatus til $status")
    }

    @DeleteMapping("/fakturaserie/{fakturaserieReferanse}")
    fun kansellerFakturaserie(
        @PathVariable("fakturaserieReferanse", required = true) referanse: String,
    ): ResponseEntity<NyFakturaserieResponseDto> {
        log.info("Mottatt ADMIN forespørsel om kansellering av fakturaserie: $referanse")
        //Sjekk at dato er 8. august
        /*if (naisClusterName != NAIS_CLUSTER_NAME_DEV) {
            log.warn("Endepunktet er kun tilgjengelig i testmiljø")
            return ResponseEntity.status(403)
                .body(NyFakturaserieResponseDto("Endepunktet er kun tilgjengelig i testmiljø"))
        }*/

        val dato = LocalDate.now()
        if (dato.month != Month.AUGUST || dato.dayOfMonth != 11 || dato.year != 2025) {
            log.warn("Endepunktet er kun tilgjengelig 11. august")
            return ResponseEntity.status(403)
                .body(NyFakturaserieResponseDto("Endepunkt er kun tilgjengelig 8. august"))
        }

        val nyFakturaserieRefereanse = faktureringService.kansellerFakturaserie(referanse)

        log.info("Kansellert fakturaserie med referanse ${referanse}, Ny fakturaseriereferanse: $nyFakturaserieRefereanse")
        return ResponseEntity.ok(NyFakturaserieResponseDto(nyFakturaserieRefereanse))
    }

    @Operation(summary = "Henter fakturaserie på referanse")
    @GetMapping("/fakturaserie/{referanse}")
    fun hentFakturaserie(@PathVariable("referanse") referanse: String): FakturaserieResponseDto {
        return faktureringService.hentFakturaserie(referanse).tilFakturaserieResponseDto(inkluderFodselsnummer = false)
    }

    @Operation(summary = "Henter faktura med ekstern faktura status og fakturaserie referanse")
    @GetMapping("/faktura/status")
    fun hentFakturaMedStatus(
        @RequestParam status: FakturaStatus
    ): ResponseEntity<List<FakturaAdminDto>> {

        if (status !in listOf(FakturaStatus.FEIL, FakturaStatus.MANGLENDE_INNBETALING)) {
            throw IllegalArgumentException("Kun FEIL og MANGLENDE_INNBETALING er tillatt som status")
        }

        val fakturaer = fakturaService.hentFakturaerMedStatus(status)
        return ResponseEntity.ok(fakturaer.map { it.toFakturaAdminDto() })
    }

    @Operation(
        summary = "Henter avstemmingsdata som CSV for fakturaer med status BESTILT eller MANGLENDE_INNBETALING i en gitt periode",
        description = "Returnerer en CSV-fil med avstemmingsdata filtrert på bestillingsdato. Bruk for eksempel Q1 2024: periodeFra=2024-01-01, periodeTil=2024-03-31"
    )
    @GetMapping("/avstemming/csv", produces = ["text/csv"])
    fun hentAvstemmingCsv(
        @Parameter(description = "Fra-dato for perioden (inklusiv)", example = "2024-01-01")
        @RequestParam periodeFra: LocalDate,
        @Parameter(description = "Til-dato for perioden (inklusiv)", example = "2024-03-31")
        @RequestParam periodeTil: LocalDate
    ): ResponseEntity<String> {
        log.info("Henter avstemmingsdata som CSV for periode $periodeFra - $periodeTil")
        val data = fakturaRepository.hentAvstemmingData(periodeFra, periodeTil)
            .map { AvstemmingCsvRad.fra(it) }

        val csvMapper = CsvMapper().apply {
            registerModule(JavaTimeModule())
        }
        val schema = csvMapper
            .schemaFor(AvstemmingCsvRad::class.java)
            .withHeader()
            .withColumnSeparator(';')

        val csv = csvMapper.writer(schema).writeValueAsString(data)

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=avstemming_${periodeFra}_${periodeTil}.csv")
            .body(csv)
    }

    companion object {
        private const val NAIS_CLUSTER_NAME_DEV = "dev-gcp"
    }
}

data class ManglendeInnbetalingSimuleringDto(
    val betaltBelop: BigDecimal,
    val fakturaNummer: String
)


