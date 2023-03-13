package no.nav.faktureringskomponenten.service.avregning

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BeløpAvregner {
    companion object {
        fun regnForPeriode(enhetspris: BigDecimal, fom: LocalDate, tom: LocalDate): BigDecimal {
            val erSammeMåned = fom.monthValue == tom.monthValue
            val antall = if (erSammeMåned) regnForMånedPeriode(fom, tom) else regnForLengrePeriode(fom, tom)
            return enhetspris.multiplyWithScaleTwo(antall)
        }

        private fun regnForLengrePeriode(fom: LocalDate, tom: LocalDate): BigDecimal {
            val antallDagerFørsteMåned = fom.lengthOfMonth() - fom.dayOfMonth + 1
            val differanseAvManed = tom.monthValue - fom.monthValue - 1
            return BigDecimal(antallDagerFørsteMåned.toDouble() / fom.lengthOfMonth() + differanseAvManed + tom.dayOfMonth.toDouble() / tom.lengthOfMonth())
                .setScale(2, RoundingMode.HALF_UP)
        }

        private fun regnForMånedPeriode(periodeFra: LocalDate, periodeTil: LocalDate): BigDecimal {
            val antallDager = BigDecimal(ChronoUnit.DAYS.between(periodeFra, periodeTil) + 1)
            val månedLengde = BigDecimal(periodeFra.lengthOfMonth())
            return antallDager.divideWithScaleTwo(månedLengde)
        }
    }
}