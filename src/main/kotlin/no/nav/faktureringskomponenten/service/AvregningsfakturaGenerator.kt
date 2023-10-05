package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import java.math.BigDecimal
import java.text.DecimalFormat

class AvregningsfakturaGenerator {
    private val decimalFormat = DecimalFormat("0.00")
    fun lagFaktura(avregningsperioder: List<Avregningsperiode>): Faktura? {
        if (avregningsperioder.isEmpty()) return null

        val fakturaLinjer = avregningsperioder.map {
            FakturaLinje(
                id = null,
                referertFakturaVedAvregning = it.bestilteFaktura,
                periodeFra = it.periodeFra,
                periodeTil = it.periodeTil,
                beskrivelse = "nytt beløp: ${decimalFormat.format(it.nyttBeløp)} - tidligere beløp: ${decimalFormat.format(it.tidligereBeløp)}",
                antall = BigDecimal(1),
                enhetsprisPerManed = it.nyttBeløp - it.tidligereBeløp,
                belop = it.nyttBeløp - it.tidligereBeløp,
            )
        }
        return Faktura(fakturaLinje = fakturaLinjer)
    }
}
