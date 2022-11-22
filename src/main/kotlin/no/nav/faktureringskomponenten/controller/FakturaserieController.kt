package no.nav.faktureringskomponenten.controller

import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.service.FaktureringService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
@RequestMapping("/fakturaserie")
@Validated
class FakturaserieController @Autowired constructor(
    val faktureringService: FaktureringService
) {

    @PostMapping // Type endres senere til FakturaserieDto
    @Validated
    fun lagNyFakturaserie(@Valid @RequestBody fakturering: FakturaserieDto): String {
        return faktureringService.lagNyFaktura()
    }
}