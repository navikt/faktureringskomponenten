package no.nav.faktureringskomponenten.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import mu.KotlinLogging
import no.nav.faktureringskomponenten.service.statistikk.FakturastatistikkDto
import no.nav.faktureringskomponenten.service.statistikk.FakturastatistikkService
import no.nav.security.token.support.core.api.Protected
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Protected
@RestController
@RequestMapping("/admin/statistikk")
class AdminStatistikkController(
    private val fakturastatistikkService: FakturastatistikkService,
) {

    @Operation(
        summary = "Hent fakturastatistikk",
        description = "Aggregerte tall for fakturaserier og fakturaer, til oversikten i melosys-console. " +
            "Driftstallene (forfalte, ventende på kvittering, feil, mottak), planlagte fakturaer og " +
            "månedsoversikten er nåtilstand. Bestilte fakturaer filtreres på bestillingsdato og " +
            "fakturaserier på opprettelsesdato med fraOgMed/tilOgMed (begge valgfrie; utelat for alt). " +
            "Svaret inneholder ingen fødselsnumre, navn eller organisasjonsnumre."
    )
    @ApiResponse(responseCode = "200", description = "Statistikk hentet")
    @ApiResponse(responseCode = "400", description = "fraOgMed er etter tilOgMed")
    @GetMapping
    fun hentFakturastatistikk(
        @Parameter(description = "Fra og med dato (inklusiv)", example = "2026-01-01")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fraOgMed: LocalDate?,
        @Parameter(description = "Til og med dato (inklusiv)", example = "2026-12-31")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) tilOgMed: LocalDate?,
    ): FakturastatistikkDto {
        if (fraOgMed != null && tilOgMed != null && fraOgMed.isAfter(tilOgMed)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "fraOgMed kan ikke være etter tilOgMed")
        }
        log.info { "Admin: Henter fakturastatistikk (fraOgMed=$fraOgMed, tilOgMed=$tilOgMed)" }
        return fakturastatistikkService.hentStatistikk(fraOgMed, tilOgMed)
    }
}
