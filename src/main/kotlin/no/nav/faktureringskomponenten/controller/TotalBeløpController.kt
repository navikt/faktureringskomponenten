package no.nav.faktureringskomponenten.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import mu.KotlinLogging
import no.nav.faktureringskomponenten.controller.dto.BeregnTotalBeløpDto
import no.nav.faktureringskomponenten.service.beregning.BeløpBeregner
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

private val log = KotlinLogging.logger { }

@Validated
@RestController
@RequestMapping("/totalbeloep")
@Protected
class TotalBeløpController {

    @Operation(summary = "Beregner totalbeløp for perioder")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Bigdecimal med totalbeløp"),
            ApiResponse(responseCode = "400", description = "Feil med validering av felter"),
            ApiResponse(responseCode = "500", description = "Feil ved kalkulering av totalbeløp")
        ]
    )
    @PostMapping("/beregn")
    fun hentTotalBeløp(
        @RequestBody @Validated beregnTotalBeløpDto: BeregnTotalBeløpDto,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        if (bindingResult.hasErrors()) {
            val errors = bindingResult.allErrors.map { it.defaultMessage }
            log.error { "Validation errors: $errors" }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors)
        }

        return try {
            val totalBeløp = BeløpBeregner.totalBeløpForAllePerioder(beregnTotalBeløpDto.fakturaseriePerioder)
            ResponseEntity.ok(totalBeløp)
        } catch (e: Exception) {
            log.error("Feil ved kalkulering av totalbeløp", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Feil ved kalkulering av totalbeløp")
        }
    }
}
