package no.nav.faktureringskomponenten.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class ExternalErrorException(message: String) : RuntimeException(message) {

}