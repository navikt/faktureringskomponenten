package no.nav.faktureringskomponenten.validators

import javax.validation.Constraint
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [DuplikatVedtaksIdValidator::class])
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class IkkeDuplikatVedtaksId(
    val message: String = "Kan ikke opprette fakturaserie når vedtaksId allerede finnes",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Any>> = []
)