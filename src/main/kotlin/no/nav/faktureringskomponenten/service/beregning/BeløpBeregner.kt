package no.nav.faktureringskomponenten.service.beregning

import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private val log = KotlinLogging.logger { }


class BeløpBeregner {
    companion object {
        fun beløpForPeriode(enhetspris: BigDecimal, fom: LocalDate, tom: LocalDate): BigDecimal {
            val antallMåneder = AntallMdBeregner(fom, tom).beregn()
            val beløp = enhetspris.multiply(antallMåneder).setScale(2, RoundingMode.UNNECESSARY)
            log.debug { "Beløp for periode fom: $fom, tom: $tom regnes med enhetspris $enhetspris og antall: $antallMåneder ==> beløp: $beløp" }
            return beløp
        }

    }
}
