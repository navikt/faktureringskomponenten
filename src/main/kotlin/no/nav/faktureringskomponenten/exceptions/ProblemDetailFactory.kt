package no.nav.faktureringskomponenten.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.validation.BindingResult

class ProblemDetailFactory {
    companion object {

        fun of(bindingResult: BindingResult): ProblemDetail {
            if (!bindingResult.hasErrors()) {
                return ProblemDetail.forStatus(HttpStatus.OK)
            }

            val problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
            problemDetail.apply {
                title = "Constraint Violation"
            }
            problemDetail.setProperty(
                "violations",
                bindingResult.fieldErrors.map {
                    mapOf("field" to it.field, "message" to it.defaultMessage, "argument" to it.rejectedValue)
                }
            )
            return problemDetail
        }

        fun mapTilProblemDetail(field: String, message: String): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
            problemDetail.apply {
                title = "Constraint Violation"
                setProperty(
                    "violations",
                    mapOf(
                        "field" to field,
                        "message" to message
                    )
                )
            }
            return problemDetail
        }
    }
}
