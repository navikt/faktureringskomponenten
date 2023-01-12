package no.nav.faktureringskomponenten.validators

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.faktureringskomponenten.service.FakturaserieService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DuplikatVedtaksIdValidator(@Autowired val fakturaserieService: FakturaserieService) :

ConstraintValidator<IkkeDuplikatVedtaksId?, String> {
    override fun initialize(constraintAnnotation: IkkeDuplikatVedtaksId?) {}
    override fun isValid(
        vedtaksId: String,
        cxt: ConstraintValidatorContext
    ): Boolean {
        return !fakturaserieService.finnesVedtaksId(vedtaksId)
    }
}