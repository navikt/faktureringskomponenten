package no.nav.faktureringskomponenten.controller.validators

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [FodselsnummerValidator::class])
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ErFodselsnummer(
    val message: String = "Fødselsnummeret er ikke gyldig",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)