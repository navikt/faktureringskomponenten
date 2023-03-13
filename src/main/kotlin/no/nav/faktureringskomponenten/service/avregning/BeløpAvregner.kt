package no.nav.faktureringskomponenten.service.avregning

import java.math.BigDecimal
import java.time.LocalDate

class BeløpAvregner {
    companion object {
        fun regnForPeriode(enhetspris: BigDecimal, fom: LocalDate, tom: LocalDate): BigDecimal {
            val angittAntall = AngittAntallBeregner.regnAngittAntallForPeriode(fom, tom)
            return enhetspris.multiplyWithScaleTwo(angittAntall)
        }
    }
}