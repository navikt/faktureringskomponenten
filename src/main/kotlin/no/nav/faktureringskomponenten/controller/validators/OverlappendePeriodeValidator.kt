package no.nav.faktureringskomponenten.controller.validators

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto


class OverlappendePeriodeValidator : ConstraintValidator<ErIkkeOverlappendePerioder?, List<FakturaseriePeriodeDto>> {
    override fun initialize(constraintAnnotation: ErIkkeOverlappendePerioder?) {}
    override fun isValid(
        perioder: List<FakturaseriePeriodeDto>,
        cxt: ConstraintValidatorContext
    ): Boolean {
        for (i in 0 until perioder.size - 1) {
            for (n in i + 1 until perioder.size) {
                if ((perioder[i].sluttDato >= perioder[n].startDato && perioder[i].startDato <= perioder[n].sluttDato) ||
                    (perioder[i].sluttDato == perioder[n].startDato || perioder[i].startDato == perioder[n].sluttDato)
                ) {
                    return false
                }
            }
        }
        return true
    }
}