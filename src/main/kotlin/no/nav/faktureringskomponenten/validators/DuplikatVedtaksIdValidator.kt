package no.nav.faktureringskomponenten.validators

import no.nav.faktureringskomponenten.service.FakturaserieService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

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