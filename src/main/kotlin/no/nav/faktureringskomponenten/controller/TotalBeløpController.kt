package no.nav.faktureringskomponenten.controller

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

    @PostMapping("/beregn")
    fun hentTotalBeløp(
        @RequestBody @Validated beregnTotalBeløpDto: BeregnTotalBeløpDto,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        log.info { "Beregner totalbeløp for" }

        if (bindingResult.hasErrors()) {
            val errors = bindingResult.allErrors.map { it.defaultMessage }
            log.error { "Validation errors: $errors" }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors)
        }

        return try {
            val totalBeløp = BeløpBeregner.totalBeløpForAllePerioder(beregnTotalBeløpDto.fakturaseriePerioder)
            ResponseEntity.ok(totalBeløp)
        } catch (e: ArithmeticException) {
            log.error("Arithmetic error during calculation", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid numerical input")
        } catch (e: Exception) {
            log.error("Error calculating total amount", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred")
        }
    }
}
