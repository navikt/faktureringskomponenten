package no.nav.faktureringskomponenten.controller.validators

import jakarta.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [DuplikatReferanseValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class IkkeDuplikatReferanse(
    val message: String = "Kan ikke opprette fakturaserie når referanse allerede finnes",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)