package no.nav.faktureringskomponenten.controller

import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.service.FakturaserieService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Validated
@RestController
@RequestMapping("/fakturaserie")
class FakturaserieController @Autowired constructor(
    val faktureringService: FakturaserieService
) {

    @PostMapping // Type endres senere til FakturaserieDto
    fun lagNyFakturaserie(@RequestBody @Valid fakturaserie: FakturaserieDto): String {
        return faktureringService.lagNyFaktura(fakturaserie)
    }
}