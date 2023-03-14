package no.nav.faktureringskomponenten.service.beregning

import java.math.BigDecimal
import java.time.LocalDate

class BeløpBeregner {
    companion object {
        fun beløpForPeriode(enhetspris: BigDecimal, fom: LocalDate, tom: LocalDate): BigDecimal {
            val angittAntall = AntallBeregner.antallForPeriode(fom, tom)
            return enhetspris.multiplyWithScaleTwo(angittAntall)
        }
    }
}