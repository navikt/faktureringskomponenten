package no.nav.faktureringskomponenten.controller.validators

import io.getunleash.Unleash
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class StartdatoErIkkeFraTidligereÅrValidator(
    @Autowired private val unleash: Unleash
) : ConstraintValidator<StartdatoErIkkeFraTidligereÅr, LocalDate> {

    override fun initialize(constraintAnnotation: StartdatoErIkkeFraTidligereÅr) {}

    override fun isValid(startDato: LocalDate, context: ConstraintValidatorContext): Boolean {
        if (!unleash.isEnabled("melosys.faktureringskomponenten.ikke-tidligere-perioder")) {
            return true
        }

        val periodeStart = LocalDate.now().withDayOfYear(1)
        return !startDato.isBefore(periodeStart)
    }
}
