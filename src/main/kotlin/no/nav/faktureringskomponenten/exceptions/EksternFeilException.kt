package no.nav.faktureringskomponenten.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class EksternFeilException(message: String) : RuntimeException(message) {

}
