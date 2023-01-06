package no.nav.faktureringskomponenten.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.service.FakturaserieService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Unprotected
@Validated
@RestController
@RequestMapping("/fakturaserie")
class FakturaserieController @Autowired constructor(
    val faktureringService: FakturaserieService,
) {

    @Operation(summary = "Lager en ny fakturaserie")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Fakturaserie opprettet"),
            ApiResponse(responseCode = "400", description = "Feil med validering av felter")
        ]
    )
    @PostMapping
    fun lagNyFakturaserie(@RequestBody @Valid fakturaserieDto: FakturaserieDto): Fakturaserie {
        return faktureringService.lagNyFakturaserie(fakturaserieDto)
    }

    @Operation(summary = "Kansellerer eksisterende fakturaserie og fremtidlige planlagte fakturaer som ikke er bestilt. " +
            "Oppretter så ny fakturaserie med fakturaer som erstatter kansellerte", description = "vedtaksId i parameter må være identifikator for fakturaserie som skal oppdateres")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "202", description = "Fakturaserie erstattet"),
            ApiResponse(responseCode = "400", description = "Feil med validering av felter")
        ]
    )
    @PutMapping("/{vedtaksId}")
    fun endreFakturaserie(
        @PathVariable("vedtaksId") vedtaksId: String,
        @RequestBody @Valid fakturaserieDto: FakturaserieDto
    ): Fakturaserie? {
        return faktureringService.endreFakturaserie(vedtaksId, fakturaserieDto)
    }

    @Operation(summary = "Henter fakturaserie på vedtaksId")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "400", description = "Fant ikke forespurt fakturaserie")
        ]
    )
    @GetMapping("/{vedtaksId}")
    fun hentFakturaserie(@PathVariable("vedtaksId") vedtaksId: String): Fakturaserie {
        return faktureringService.hentFakturaserie(vedtaksId)
    }
}