package no.nav.faktureringskomponenten.controller


import no.nav.faktureringskomponenten.controller.dto.FakturaTilbakemeldingResponseDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturaTilbakemeldingResponseDto
import no.nav.faktureringskomponenten.service.FakturaMottattService
import no.nav.security.token.support.core.api.Protected
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Protected
@Validated
@RestController
@RequestMapping("/faktura")
class FakturaController @Autowired constructor(
    val fakturaMottattService: FakturaMottattService
) {
    @GetMapping("/{fakturaNr}")
    fun hentFakturastatus(@PathVariable("fakturaNr") fakturaNr: String): List<FakturaTilbakemeldingResponseDto>? {
        return fakturaMottattService.hentFakturamottat(fakturaNr)?.map { it.tilFakturaTilbakemeldingResponseDto }
    }
}