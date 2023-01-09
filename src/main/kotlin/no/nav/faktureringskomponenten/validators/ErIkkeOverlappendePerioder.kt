package no.nav.faktureringskomponenten.validators

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [OverlappendePeriodeValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ErIkkeOverlappendePerioder(
    val message: String = "Periodene kan ikke overlappes",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)