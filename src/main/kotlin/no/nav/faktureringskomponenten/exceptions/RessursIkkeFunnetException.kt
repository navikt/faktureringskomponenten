package no.nav.faktureringskomponenten.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class RessursIkkeFunnetException: RuntimeException {

    var title: String = ""
    var field: String = ""

    constructor(title: String = "Fant ikke ressurs", field: String, message: String): super(message) {
        this.title = title
        this.field = field
    }
}
