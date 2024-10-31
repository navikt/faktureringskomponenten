package no.nav.faktureringskomponenten.controller

import mu.KotlinLogging
import no.nav.faktureringskomponenten.controller.dto.EnkeltFakturaRequestDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturaRequest
import no.nav.faktureringskomponenten.exceptions.ProblemDetailFactory
import no.nav.faktureringskomponenten.service.FakturaserieService
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

@Protected
@Validated
@RestController
@RequestMapping("/faktura")
class FakturaController @Autowired constructor(
    val fakturaserieService: FakturaserieService
) {


    @ProtectedWithClaims(issuer = "aad", claimMap = ["roles=faktureringskomponenten-skriv"])
    @PostMapping
    fun lagFaktura(
        @RequestBody @Validated fakturaRequestDto: EnkeltFakturaRequestDto,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        log.info("Mottatt $fakturaRequestDto")

        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ProblemDetailFactory.of(bindingResult))
        }

        val referanse = fakturaserieService.lagNyFaktura(fakturaRequestDto.tilFakturaRequest)
        return ResponseEntity.ok(referanse)
    }
}