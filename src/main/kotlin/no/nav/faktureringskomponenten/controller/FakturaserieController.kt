package no.nav.faktureringskomponenten.controller

import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.service.FakturaserieService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@RequestMapping("/fakturaserie")
class FakturaserieController @Autowired constructor(
    val faktureringService: FakturaserieService,
) {

    @PostMapping
    fun lagNyFakturaserie(@RequestBody @Valid fakturaserieDto: FakturaserieDto): Fakturaserie {
        return faktureringService.lagNyFakturaserie(fakturaserieDto)
    }

    @PutMapping("/{vedtaksId}")
    fun endreFakturaserie(
        @PathVariable("vedtaksId") vedtaksId: String,
        @RequestBody @Valid fakturaserieDto: FakturaserieDto
    ): Fakturaserie? {
        return faktureringService.endreFakturaserie(vedtaksId, fakturaserieDto)
    }

    @GetMapping("/{vedtaksId}")
    fun hentFakturaserie(@PathVariable("vedtaksId") vedtaksId: String): Fakturaserie {
        return faktureringService.hentFakturaserie(vedtaksId)
    }
}