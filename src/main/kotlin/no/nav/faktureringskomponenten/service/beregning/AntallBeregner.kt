package no.nav.faktureringskomponenten.service.beregning

import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger { }

class AntallBeregner(private val fom: LocalDate, private val tom: LocalDate) {

    private val førsteMånedDager = fom.lengthOfMonth().toBigDecimal()
    private val sisteMånedDager = tom.lengthOfMonth().toBigDecimal()
    private val erSammeMånedOgÅr = fom.year == tom.year && fom.monthValue == tom.monthValue

    fun beregn(): BigDecimal {
        val total = beregnFørsteMånedProsent() + beregnMånederMellomProsent() + beregnSisteMånedProsent()
        log.info("AntallBeregner fom: {}, tom: {} som gir angitt antall total: {}", fom, tom, total)
        return total
    }

    private fun beregnFørsteMånedProsent(): BigDecimal {
        return if (erSammeMånedOgÅr) {
            (tom.dayOfMonth.toBigDecimal() - fom.dayOfMonth.toBigDecimal() + BigDecimal.ONE)
                .divide(førsteMånedDager, 2, RoundingMode.HALF_UP)
        } else {
            (førsteMånedDager - fom.dayOfMonth.toBigDecimal() + BigDecimal.ONE)
                .divide(førsteMånedDager, 2, RoundingMode.HALF_UP)
        }
    }

    private fun beregnSisteMånedProsent(): BigDecimal {
        return if (erSammeMånedOgÅr) {
            BigDecimal.ZERO
        } else {
            tom.dayOfMonth.toBigDecimal().divide(sisteMånedDager, 2, RoundingMode.HALF_UP)
        }
    }

    private fun beregnMånederMellomProsent(): BigDecimal {
        return if (erSammeMånedOgÅr) {
            BigDecimal.ZERO
        } else {
            val start = fom.withDayOfMonth(1).plusMonths(1)
            val end = tom.withDayOfMonth(1)
            ChronoUnit.MONTHS.between(start, end).toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        }
    }
}