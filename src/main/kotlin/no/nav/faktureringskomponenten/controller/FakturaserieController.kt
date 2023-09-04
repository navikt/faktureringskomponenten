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
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.exceptions.ProblemDetailValidator
import no.nav.faktureringskomponenten.metrics.MetrikkNavn
import no.nav.faktureringskomponenten.service.FakturaMottattService
import no.nav.faktureringskomponenten.service.FakturaserieService
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
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
    ): ResponseEntity<ProblemDetail> {

        val responseEntity = ProblemDetailValidator.validerBindingResult(bindingResult)

        if (responseEntity.statusCode == HttpStatus.OK) {
            log.info("Mottatt $fakturaserieRequestDto")

            val forrigeReferanseId = fakturaserieRequestDto.referanseId
            val fakturaserieDto = fakturaserieRequestDto.tilFakturaserieDto
            val referanseId = faktureringService.lagNyFakturaserie(fakturaserieDto, forrigeReferanseId)

            Metrics.counter(MetrikkNavn.FAKTURASERIE_OPPRETTET).increment()

            return ProblemDetailValidator.leggTilProperties(linkedMapOf(Pair("referanseId", referanseId)))
        }

        return responseEntity
    }

    @Operation(summary = "Henter fakturaserie på referanseId")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "400", description = "Fant ikke forespurt fakturaserie")
        ]
    )
    @GetMapping("/{referanseId}")
    fun hentFakturaserie(@PathVariable("referanseId") referanseId: String): FakturaserieResponseDto {
        return faktureringService.hentFakturaserie(referanseId).tilFakturaserieResponseDto
    }

    @GetMapping
    fun hentFakturaserier(
        @RequestParam("referanseId") referanseId: String,
        @RequestParam(value = "fakturaStatus", required = false) fakturaStatus: String? = null
): List<FakturaserieResponseDto> {
        return faktureringService.hentFakturaserier(referanseId, fakturaStatus).map { it.tilFakturaserieResponseDto }
    }
}
