package no.nav.faktureringskomponenten.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class RessursIkkeFunnetException: RuntimeException {

    var tittel: String = ""
    var felt: String = ""

    constructor(tittel: String = "Fant ikke ressurs", felt: String, melding: String): super(melding) {
        this.tittel = tittel
        this.felt = felt
    }
}