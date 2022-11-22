package no.nav.faktureringskomponenten.validators

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext


class FodselsnummerValidator  : ConstraintValidator<ErFodselsnummer?, String>    {
    override fun initialize(constraintAnnotation: ErFodselsnummer?) {}
    override fun isValid(
        fodselsnummer: String,
        cxt: ConstraintValidatorContext
    ): Boolean {
        return fodselsnummer.matches(Regex("[0-9]+")) && fodselsnummer.length == 11
    }
}