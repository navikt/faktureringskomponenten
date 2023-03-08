package no.nav.faktureringskomponenten.controller.validators

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext


class FodselsnummerValidator  : ConstraintValidator<ErFodselsnummer?, String>    {
    override fun initialize(constraintAnnotation: ErFodselsnummer?) {}
    override fun isValid(
        fodselsnummer: String,
        cxt: ConstraintValidatorContext
    ): Boolean {
        return fodselsnummer.matches(Regex("[0-9]+")) && fodselsnummer.length == 11
    }
}