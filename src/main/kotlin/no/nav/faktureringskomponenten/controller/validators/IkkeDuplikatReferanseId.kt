package no.nav.faktureringskomponenten.controller.validators

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [DuplikatReferanseIdValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class IkkeDuplikatReferanseId(
    val message: String = "Kan ikke opprette fakturaserie når referanseId allerede finnes",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)