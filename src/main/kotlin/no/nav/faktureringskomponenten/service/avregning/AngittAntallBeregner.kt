package no.nav.faktureringskomponenten.service.avregning

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class AngittAntallBeregner {
    companion object {
        fun regnAngittAntallForPeriode(fom: LocalDate, tom: LocalDate): BigDecimal {
            val gjelderSammeÅr = fom.year == tom.year
            val gjelderSammeMåned = fom.monthValue == tom.monthValue
            if (gjelderSammeÅr && gjelderSammeMåned) {
                return regnAntallForEnMåned(fom, tom)
            }

            var currentDate = fom
            var totalAntall = BigDecimal.ZERO

            while (currentDate <= tom) {
                val erFørsteMåned = fom == currentDate
                val erSisteMåned = currentDate.year == tom.year && currentDate.month == tom.month
                val totalAntallDagerForMåned = currentDate.lengthOfMonth()
                val dagerIgjenAvMåneden = totalAntallDagerForMåned - (currentDate.dayOfMonth - 1)

                val brøkdelAvMåned =
                    BigDecimal(dagerIgjenAvMåneden.toDouble() / totalAntallDagerForMåned.toDouble()).setScale(
                        2,
                        RoundingMode.HALF_UP
                    )

                when {
                    erFørsteMåned -> {
                        totalAntall = brøkdelAvMåned
                        currentDate = currentDate.withDayOfMonth(1)
                    }

                    erSisteMåned -> {
                        val daysInMonth = tom.dayOfMonth - (currentDate.dayOfMonth - 1)
                        totalAntall += BigDecimal(daysInMonth.toDouble() / totalAntallDagerForMåned.toDouble()).setScale(
                            2,
                            RoundingMode.HALF_UP
                        )
                    }

                    else -> {
                        totalAntall += brøkdelAvMåned
                    }
                }

                currentDate = currentDate.plusMonths(1)
            }
            return totalAntall.setScale(2, RoundingMode.HALF_UP)
        }

        private fun regnAntallForEnMåned(periodeFra: LocalDate, periodeTil: LocalDate): BigDecimal {
            val antallDager = BigDecimal(ChronoUnit.DAYS.between(periodeFra, periodeTil) + 1)
            val månedLengde = BigDecimal(periodeFra.lengthOfMonth())
            return antallDager.divideWithScaleTwo(månedLengde)
        }
    }
}