package no.nav.faktureringskomponenten.service.beregning

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class AntallBeregner {
    companion object {
        fun antallForPeriode(fom: LocalDate, tom: LocalDate): BigDecimal {
            val gjelderSammeMånedOgÅr = fom.year == tom.year && fom.monthValue == tom.monthValue
            if (gjelderSammeMånedOgÅr) {
                return regnAngittAntallForEnMåned(fom, tom)
            }
            return regnAngittAntallForFlereMåneder(fom, tom)
        }

        private fun regnAngittAntallForFlereMåneder(fom: LocalDate, tom: LocalDate): BigDecimal {
            var totalAngittAntall = BigDecimal.ZERO
            var currentDate = fom

            while (currentDate <= tom) {
                val erFørsteMåned = fom == currentDate
                val erSisteMåned = currentDate.year == tom.year && currentDate.month == tom.month
                val totalAntallDagerForMåned = currentDate.lengthOfMonth()
                val dagerIgjenAvMåneden = totalAntallDagerForMåned - (currentDate.dayOfMonth - 1)

                val angittAntallForMåned =
                    BigDecimal(dagerIgjenAvMåneden.toDouble() / totalAntallDagerForMåned.toDouble())
                        .setScale(2, RoundingMode.HALF_UP)

                when {
                    erFørsteMåned -> {
                        totalAngittAntall = angittAntallForMåned
                        currentDate = currentDate.withDayOfMonth(1)
                    }

                    erSisteMåned -> {
                        val daysInMonth = tom.dayOfMonth - (currentDate.dayOfMonth - 1)
                        totalAngittAntall += BigDecimal(daysInMonth.toDouble() / totalAntallDagerForMåned.toDouble())
                            .setScale(2, RoundingMode.HALF_UP)
                    }

                    else -> {
                        totalAngittAntall += angittAntallForMåned
                    }
                }
                currentDate = currentDate.plusMonths(1)
            }
            return totalAngittAntall.setScale(2, RoundingMode.HALF_UP)
        }

        private fun regnAngittAntallForEnMåned(fom: LocalDate, tom: LocalDate): BigDecimal {
            val antallDagerForMåned = BigDecimal(ChronoUnit.DAYS.between(fom, tom) + 1)
            val totalAntallDagerForMåned = BigDecimal(fom.lengthOfMonth())
            return antallDagerForMåned.divideWithScaleTwo(totalAntallDagerForMåned)
        }
    }
}