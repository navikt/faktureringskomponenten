package no.nav.faktureringskomponenten.exceptions.config

import jakarta.validation.ConstraintViolationException
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class ExceptionHandler: ResponseEntityExceptionHandler() {

    @ExceptionHandler(RessursIkkeFunnetException::class)
    fun handleRessursIkkeFunnetException(ressursIkkeFunnetException: RessursIkkeFunnetException): ProblemDetail{
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ressursIkkeFunnetException.message!!)
        problemDetail.apply {
            title = ressursIkkeFunnetException.title
            detail = ressursIkkeFunnetException.field
        }
        problemDetail.setProperty("message", ressursIkkeFunnetException.message)
        return problemDetail
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ProblemDetail {
        val melding = ex.constraintViolations.joinToString(", ") { it.message }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, melding)
    }
}
