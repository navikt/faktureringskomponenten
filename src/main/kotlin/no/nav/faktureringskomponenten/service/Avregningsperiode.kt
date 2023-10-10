package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import java.math.BigDecimal
import java.time.LocalDate

data class Avregningsperiode(
    val periodeFra: LocalDate,
    val periodeTil: LocalDate,
    val bestilteFaktura: Faktura?,
    val tidligereBeløp: BigDecimal,
    val nyttBeløp: BigDecimal

) {
}
