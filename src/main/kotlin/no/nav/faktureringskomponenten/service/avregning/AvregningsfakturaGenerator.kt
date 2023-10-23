package no.nav.faktureringskomponenten.service.avregning

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import org.springframework.stereotype.Component
import ulid.ULID
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

@Component
class AvregningsfakturaGenerator {
    private val decimalFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale("no", "NO", "nb")))
    fun lagFaktura(avregningsperioder: List<Avregningsperiode>): Faktura? {
        if (avregningsperioder.isEmpty()) return null

        val fakturaLinjer = avregningsperioder.map {
            FakturaLinje(
                id = null,
                referertFakturaVedAvregning = it.bestilteFaktura,
                periodeFra = it.periodeFra,
                periodeTil = it.periodeTil,
                beskrivelse = "Tidligere fakturanummer: ${it.bestilteFaktura?.eksternFakturaNummer}, nytt beløp: ${decimalFormat.format(it.nyttBeløp)} - tidligere beløp: ${decimalFormat.format(it.tidligereBeløp)}",
                antall = BigDecimal(1),
                enhetsprisPerManed = it.nyttBeløp - it.tidligereBeløp,
                avregningForrigeBeloep = it.tidligereBeløp,
                avregningNyttBeloep = it.nyttBeløp,
                belop = it.nyttBeløp - it.tidligereBeløp,
            )
        }
        return Faktura(referanseNr = ULID.randomULID(), fakturaLinje = fakturaLinjer)
    }
}