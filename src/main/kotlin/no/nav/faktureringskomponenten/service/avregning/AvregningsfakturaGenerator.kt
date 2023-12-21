package no.nav.faktureringskomponenten.service.avregning

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
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

    fun lagFaktura(avregningsperiode: Avregningsperiode): Faktura {
        val fakturaLinje =
            FakturaLinje(
                id = null,
                referertFakturaVedAvregning = avregningsperiode.bestilteFaktura,
                periodeFra = avregningsperiode.periodeFra,
                periodeTil = avregningsperiode.periodeTil,
                beskrivelse = "Periode: ${avregningsperiode.periodeFra.format(dateFormat)} - ${
                    avregningsperiode.periodeTil.format(
                        dateFormat
                    )
                }\nNytt beløp: ${
                    decimalFormat.format(
                        avregningsperiode.nyttBeløp
                    )
                } - tidligere beløp: ${decimalFormat.format(avregningsperiode.tidligereBeløp)}",
                enhetsprisPerManed = (avregningsperiode.nyttBeløp - avregningsperiode.tidligereBeløp).abs(),
                antall = if (avregningsperiode.nyttBeløp - avregningsperiode.tidligereBeløp < BigDecimal.ZERO) BigDecimal(
                    -1
                ) else BigDecimal(1),
                avregningForrigeBeloep = avregningsperiode.tidligereBeløp,
                avregningNyttBeloep = avregningsperiode.nyttBeløp,
                belop = avregningsperiode.nyttBeløp - avregningsperiode.tidligereBeløp,
            )

        return Faktura(
            referanseNr = ULID.randomULID(),
            krediteringFakturaRef = avregningsperiode.bestilteFaktura.referanseNr,
            fakturaLinje = listOf(fakturaLinje),
            status = if (avregningsperiode.nyttBeløp.compareTo(avregningsperiode.tidligereBeløp) != 0) FakturaStatus.OPPRETTET else FakturaStatus.BESTILT
        )
    }
}