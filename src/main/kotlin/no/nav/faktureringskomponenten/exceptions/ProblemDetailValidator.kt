package no.nav.faktureringskomponenten.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult

class ProblemDetailValidator {
    companion object {

        fun validerBindingResult(bindingResult: BindingResult): ProblemDetail {
            if (bindingResult.hasErrors()) {
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

            return ProblemDetail.forStatus(HttpStatus.OK)
        }

        fun leggTilProperties(properties: LinkedHashMap<String, String>): ResponseEntity<ProblemDetail> {
            val problemDetail = ProblemDetail.forStatus(HttpStatus.OK)
            for ((key, value) in properties) {
                problemDetail.setProperty(key, value)
            }
            return ResponseEntity<ProblemDetail>(problemDetail, HttpStatus.OK)
        }
    }
}
