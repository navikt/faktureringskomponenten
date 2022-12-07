package no.nav.faktureringskomponenten.validators

import org.apiguardian.api.API
import org.hibernate.annotations.Immutable
import org.zalando.problem.Exceptional
import org.zalando.problem.Status
import org.zalando.problem.violations.ConstraintViolationProblem
import org.zalando.problem.violations.Violation
import java.net.URI

@API(status = API.Status.STABLE)
@Immutable
class RessursIkkeFunnetException(violations: MutableList<Violation>?) :
    ConstraintViolationProblem(Status.BAD_REQUEST, violations) {

    constructor(felt: String, melding: String) : this(mutableListOf(Violation(felt, melding)))

    override fun getCause(): Exceptional? {
        return null
    }

    override fun getTitle(): String {
        return "Resource not found violation"
    }

    override fun getType(): URI? {
        return null
    }
}