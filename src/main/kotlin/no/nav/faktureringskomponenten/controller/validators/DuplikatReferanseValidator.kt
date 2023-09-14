package no.nav.faktureringskomponenten.controller.validators

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.faktureringskomponenten.service.FakturaserieService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DuplikatReferanseValidator(@Autowired val fakturaserieService: FakturaserieService) :

ConstraintValidator<IkkeDuplikatReferanse?, String> {
    override fun initialize(constraintAnnotation: IkkeDuplikatReferanse?) {}
    override fun isValid(
        referanse: String,
        cxt: ConstraintValidatorContext
    ): Boolean {
        return !fakturaserieService.finnesReferanse(referanse)
    }
}