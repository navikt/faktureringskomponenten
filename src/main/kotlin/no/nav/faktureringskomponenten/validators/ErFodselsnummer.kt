package no.nav.faktureringskomponenten.validators

import javax.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [FodselsnummerValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ErFodselsnummer(
    val message: String = "Fødselsnummeret er ikke gyldig",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)