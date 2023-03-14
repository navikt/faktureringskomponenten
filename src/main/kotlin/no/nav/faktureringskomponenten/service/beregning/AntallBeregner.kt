package no.nav.faktureringskomponenten.service.beregning

import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger { }

class AntallBeregner {
    companion object {
        fun antallForPeriode(fom: LocalDate, tom: LocalDate): BigDecimal {
            val gjelderSammeMånedOgÅr = fom.year == tom.year && fom.monthValue == tom.monthValue
            if (gjelderSammeMånedOgÅr) {
                log.debug("Beregner antall for periode $fom og $tom på én og samme måned")
                return beregnAntallForEnMåned(fom, tom)
            }
            log.debug("Beregner antall for periode $fom og $tom på flere måneder")
            return beregnAntallForFlereMåneder(fom, tom)
        }

        private fun beregnAntallForFlereMåneder(fom: LocalDate, tom: LocalDate): BigDecimal {
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

        private fun beregnAntallForEnMåned(fom: LocalDate, tom: LocalDate): BigDecimal {
            val antallDagerForMåned = BigDecimal(ChronoUnit.DAYS.between(fom, tom) + 1)
            val totalAntallDagerForMåned = BigDecimal(fom.lengthOfMonth())
            return antallDagerForMåned.divideWithScaleTwo(totalAntallDagerForMåned)
        }
    }
}