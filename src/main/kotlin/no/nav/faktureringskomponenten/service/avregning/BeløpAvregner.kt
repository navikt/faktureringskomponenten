package no.nav.faktureringskomponenten.service.avregning

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class BeløpAvregner {
    companion object {
        fun regnForPeriode(enhetspris: BigDecimal, fom: LocalDate, tom: LocalDate): BigDecimal {
            val gjelderSammeÅr = fom.year == tom.year
            val gjelderSammeMåned = fom.monthValue == tom.monthValue
            val antall: BigDecimal
            if (gjelderSammeÅr && gjelderSammeMåned) {
                antall = regnForMånedPeriode(fom, tom)
            } else if(gjelderSammeÅr) {
                antall = regnForLengrePeriode(fom, tom)
            } else {
                antall = regnForEndaLengrePeriode(fom, tom)
            }

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

        private fun regnForEndaLengrePeriode(fom: LocalDate, tom: LocalDate): BigDecimal {

            var currentDate = fom
            var totalAntall = BigDecimal.ZERO

            while (currentDate <= tom) {
                val erSisteMåneden = currentDate.year == tom.year && currentDate.month == tom.month
                if (fom == currentDate) {
                    val totalAntallDagerForMåneden = currentDate.lengthOfMonth()
                    val antallDagerForMåneden = totalAntallDagerForMåneden - (currentDate.dayOfMonth - 1)
                    totalAntall = BigDecimal(antallDagerForMåneden.toDouble() / totalAntallDagerForMåneden.toDouble()).setScale(2, RoundingMode.HALF_UP)
                    currentDate = currentDate.withDayOfMonth(1)
                }
                else if (erSisteMåneden) {
                    val totalAntallDagerForMåneden = currentDate.lengthOfMonth()
                    val antallDagerForMåneden = tom.dayOfMonth - (currentDate.dayOfMonth - 1)
                    val antall = BigDecimal(antallDagerForMåneden.toDouble() / totalAntallDagerForMåneden.toDouble()).setScale(2, RoundingMode.HALF_UP)
                    totalAntall = totalAntall.add(antall)
                }
                else {
                    val totalAntallDagerForMåneden = currentDate.lengthOfMonth()
                    val antallDagerForMåneden = totalAntallDagerForMåneden - (currentDate.dayOfMonth - 1)
                    val antall = BigDecimal(antallDagerForMåneden.toDouble() / totalAntallDagerForMåneden.toDouble()).setScale(2, RoundingMode.HALF_UP)
                    totalAntall = totalAntall.add(antall)
                }
                currentDate = currentDate.plusMonths(1)
            }

            return totalAntall.setScale(2, RoundingMode.HALF_UP)
        }
    }
}