package no.nav.faktureringskomponenten.service.beregning

import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger { }

class AntallBeregner {
    companion object {
        fun antallForPeriode(datoFra: LocalDate, datoTil: LocalDate): BigDecimal {
            val førsteMånedDager = datoFra.lengthOfMonth().toBigDecimal()
            val sisteMånedDager = datoTil.lengthOfMonth().toBigDecimal()

            val erSammeMånedOgÅr = datoFra.year == datoTil.year && datoFra.monthValue == datoTil.monthValue
            val førsteMånedProsent = beregnProsentTilFørsteMåned(erSammeMånedOgÅr, førsteMånedDager, datoFra, datoTil)
            val sisteMånedProsent = beregnProsentTilSisteMåned(erSammeMånedOgÅr, datoTil, sisteMånedDager)
            val månederMellom = beregnProsentTilMånederMellomFørsteOgSiste(erSammeMånedOgÅr, datoFra, datoTil)

            val total = førsteMånedProsent + månederMellom + sisteMånedProsent
            return total.setScale(2, RoundingMode.HALF_UP)
        }

        private fun beregnProsentTilMånederMellomFørsteOgSiste(
            erSammeMånedOgÅr: Boolean,
            datoFra: LocalDate,
            datoTil: LocalDate
        ): BigDecimal {
            return if (erSammeMånedOgÅr) {
                BigDecimal.ZERO
            } else {
                val start = datoFra.withDayOfMonth(1).plusMonths(1)
                val end = datoTil.withDayOfMonth(1)
                ChronoUnit.MONTHS.between(start, end).toBigDecimal()
            }
        }

        private fun beregnProsentTilSisteMåned(
            erSammeMånedOgÅr: Boolean,
            datoTil: LocalDate,
            sisteMånedDager: BigDecimal
        ): BigDecimal {
            return if (!erSammeMånedOgÅr) {
                datoTil.dayOfMonth.toBigDecimal()
                    .divide(sisteMånedDager, 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
        }

        private fun beregnProsentTilFørsteMåned(
            erSammeMånedOgÅr: Boolean,
            førsteMånedDager: BigDecimal,
            datoFra: LocalDate,
            datoTil: LocalDate
        ): BigDecimal {
            return if (!erSammeMånedOgÅr) {
                (førsteMånedDager - datoFra.dayOfMonth.toBigDecimal() + BigDecimal.ONE)
                    .divide(førsteMånedDager, 2, RoundingMode.HALF_UP)
            } else {
                (datoTil.dayOfMonth.toBigDecimal() - datoFra.dayOfMonth.toBigDecimal() + BigDecimal.ONE)
                    .divide(førsteMånedDager, 2, RoundingMode.HALF_UP)
            }
        }
    }
}