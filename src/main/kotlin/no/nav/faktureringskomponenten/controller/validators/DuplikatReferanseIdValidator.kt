package no.nav.faktureringskomponenten.controller.validators

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.faktureringskomponenten.service.FakturaserieService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DuplikatReferanseIdValidator(@Autowired val fakturaserieService: FakturaserieService) :

ConstraintValidator<IkkeDuplikatReferanseId?, String> {
    override fun initialize(constraintAnnotation: IkkeDuplikatReferanseId?) {}
    override fun isValid(
        referanseId: String,
        cxt: ConstraintValidatorContext
    ): Boolean {
        return !fakturaserieService.finnesReferanseId(referanseId)
    }
}