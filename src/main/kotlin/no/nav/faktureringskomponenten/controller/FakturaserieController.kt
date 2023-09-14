package no.nav.faktureringskomponenten.controller

import io.micrometer.core.instrument.Metrics
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import mu.KotlinLogging
import no.nav.faktureringskomponenten.controller.dto.FakturaserieRequestDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturaserieDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturaserieResponseDto
import no.nav.faktureringskomponenten.exceptions.ProblemDetailFactory
import no.nav.faktureringskomponenten.metrics.MetrikkNavn
import no.nav.faktureringskomponenten.service.FakturaMottattService
import no.nav.faktureringskomponenten.service.FakturaserieService
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*


private val log = KotlinLogging.logger { }

@Protected
@Validated
@RestController
@RequestMapping("/fakturaserier")
class FakturaserieController @Autowired constructor(
    val faktureringService: FakturaserieService,
    val fakturaMottattService: FakturaMottattService
) {

    @Operation(summary = "Lager en ny fakturaserie")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Fakturaserie opprettet"),
            ApiResponse(responseCode = "400", description = "Feil med validering av felter")
        ]
    )

    @ProtectedWithClaims(issuer = "aad", claimMap = ["roles=faktureringskomponenten-skriv"])
    @PostMapping
    fun lagNyFakturaserie(
        @RequestBody @Validated fakturaserieRequestDto: FakturaserieRequestDto,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        log.info("Mottatt $fakturaserieRequestDto")

        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ProblemDetailFactory.of(bindingResult))
        }

        val forrigeReferanse = fakturaserieRequestDto.fakturaserieReferanse
        val fakturaserieDto = fakturaserieRequestDto.tilFakturaserieDto
        val referanse = faktureringService.lagNyFakturaserie(fakturaserieDto, forrigeReferanse)
        Metrics.counter(MetrikkNavn.FAKTURASERIE_OPPRETTET).increment()

        return ResponseEntity.ok(NyFakturaserieResponseDto(referanse))
    }

    @Operation(summary = "Henter fakturaserie på referanse")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "400", description = "Fant ikke forespurt fakturaserie")
        ]
    )
    @GetMapping("/{referanse}")
    fun hentFakturaserie(@PathVariable("referanse") referanse: String): FakturaserieResponseDto {
        return faktureringService.hentFakturaserie(referanse).tilFakturaserieResponseDto
    }

    @GetMapping
    fun hentFakturaserier(
        @RequestParam("referanse") referanse: String,
        @RequestParam(value = "fakturaStatus", required = false) fakturaStatus: String? = null
): List<FakturaserieResponseDto> {
        return faktureringService.hentFakturaserier(referanse, fakturaStatus).map { it.tilFakturaserieResponseDto }
    }
}
