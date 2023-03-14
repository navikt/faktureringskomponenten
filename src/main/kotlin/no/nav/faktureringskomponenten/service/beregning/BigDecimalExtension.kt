package no.nav.faktureringskomponenten.service.beregning

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.multiplyWithScaleTwo(other: BigDecimal): BigDecimal =
    this.multiply(other).setScale(2, RoundingMode.HALF_UP)

fun BigDecimal.divideWithScaleTwo(divisor: BigDecimal): BigDecimal =
    this.divide(divisor, 2, RoundingMode.HALF_UP)