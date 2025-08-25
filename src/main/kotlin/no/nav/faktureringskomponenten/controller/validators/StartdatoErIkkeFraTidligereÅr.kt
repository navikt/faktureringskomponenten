package no.nav.faktureringskomponenten.controller.validators

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Constraint(validatedBy = [StartdatoErIkkeFraTidligereÅrValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class StartdatoErIkkeFraTidligereÅr(
    val message: String = "Startdato kan ikke være fra tidligere år",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
