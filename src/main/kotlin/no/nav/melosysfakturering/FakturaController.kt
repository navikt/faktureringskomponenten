package no.nav.melosysfakturering

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
class FakturaController {

    @PostMapping("/ny-faktura")
    fun beregnTrygdeavgift(): Boolean {
        return true
    }

}