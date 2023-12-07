package no.nav.faktureringskomponenten.service.avregning

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import org.springframework.stereotype.Component
import ulid.ULID
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class AvregningsfakturaGenerator {
    private val decimalFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale("no", "NO", "nb")))
    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun lagFaktura(avregningsperioder: List<Avregningsperiode>): Faktura? {

        if (avregningsperioder.isEmpty()) return null

        val fakturaLinjer = avregningsperioder.map {
            FakturaLinje(
                id = null,
                referertFakturaVedAvregning = it.bestilteFaktura,
                periodeFra = it.periodeFra,
                periodeTil = it.periodeTil,
                beskrivelse = "Periode: ${it.periodeFra.format(dateFormat)} - ${it.periodeTil.format(dateFormat)}\nNytt beløp: ${decimalFormat.format(it.nyttBeløp)} - tidligere beløp: ${decimalFormat.format(it.tidligereBeløp)}",
                enhetsprisPerManed = (it.nyttBeløp - it.tidligereBeløp).abs(),
                antall = if(it.nyttBeløp - it.tidligereBeløp < BigDecimal.ZERO) BigDecimal(-1) else BigDecimal(1),
                avregningForrigeBeloep = it.tidligereBeløp,
                avregningNyttBeloep = it.nyttBeløp,
                belop = it.nyttBeløp - it.tidligereBeløp,
            )
        }
        return Faktura(referanseNr = ULID.randomULID(), fakturaLinje = fakturaLinjer.sortedByDescending { it.periodeFra })
    }
}