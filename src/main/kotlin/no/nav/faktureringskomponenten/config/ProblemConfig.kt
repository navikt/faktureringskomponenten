package no.nav.faktureringskomponenten.config

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.zalando.problem.jackson.ProblemModule
import org.zalando.problem.violations.ConstraintViolationProblemModule

@EnableAutoConfiguration(exclude = [ErrorMvcAutoConfiguration::class])
class ProblemConfig {

    @Bean
    fun problemModule(): ProblemModule {
        return ProblemModule().apply { }
    }

    @Bean
    fun constraintViolationProblemModule(): ConstraintViolationProblemModule {
        return ConstraintViolationProblemModule()
    }

}