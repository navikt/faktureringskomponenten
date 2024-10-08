package no.nav.faktureringskomponenten.controller

import no.nav.faktureringskomponenten.controller.dto.BeregnTotalBeløpDto
import no.nav.faktureringskomponenten.service.beregning.BeløpBeregner
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

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
        return ResponseEntity.ok(BeløpBeregner.totalBeløpForAllePerioder(beregnTotalBeløpDto.fakturaseriePerioder))
    }
}
