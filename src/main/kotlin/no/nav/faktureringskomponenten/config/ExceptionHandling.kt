package no.nav.faktureringskomponenten.config

import org.springframework.web.bind.annotation.ControllerAdvice
import org.zalando.problem.spring.web.advice.ProblemHandling

@ControllerAdvice
class ExceptionHandling : ProblemHandling {
}