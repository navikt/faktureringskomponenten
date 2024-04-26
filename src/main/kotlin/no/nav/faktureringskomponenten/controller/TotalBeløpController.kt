package no.nav.faktureringskomponenten.controller

import mu.KotlinLogging
import no.nav.faktureringskomponenten.controller.dto.BeregnTotalBeløpDto
import no.nav.faktureringskomponenten.controller.dto.TotalBeløpDto
import no.nav.faktureringskomponenten.service.beregning.BeløpBeregner
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal


private val log = KotlinLogging.logger { }

@Protected
@Validated
@RestController
@RequestMapping("/totalbeloep")
class TotalBeløpController {
    @ProtectedWithClaims(issuer = "aad", claimMap = ["roles=faktureringskomponenten-skriv"])
    @PostMapping
    fun hentTotalBeløp(
        @RequestBody @Validated beregnTotalBeløpDto: BeregnTotalBeløpDto,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {

        val totalSum: BigDecimal = beregnTotalBeløpDto.fakturaseriePerioder.sumOf { periode ->
            BeløpBeregner.beløpForPeriode(
                enhetspris = periode.enhetsprisPerManed,
                fom = beregnTotalBeløpDto.periodeFra,
                tom = beregnTotalBeløpDto.periodeTil
            )
        }

        return ResponseEntity.ok(TotalBeløpDto(totalSum))
    }
}
