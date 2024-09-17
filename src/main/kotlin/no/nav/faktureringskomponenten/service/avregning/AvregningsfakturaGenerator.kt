package no.nav.faktureringskomponenten.service.avregning

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import org.springframework.stereotype.Component
import ulid.ULID
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
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
                periodeFra = avregningsperiode.periodeFra,
                periodeTil = avregningsperiode.periodeTil,
                beskrivelse = genererBeskrivelse(
                    avregningsperiode.periodeFra,
                    avregningsperiode.periodeTil,
                    avregningsperiode.nyttBeløp,
                    avregningsperiode.tidligereBeløp
                ),
                enhetsprisPerManed = (avregningsperiode.nyttBeløp - avregningsperiode.tidligereBeløp).abs(),
                antall = if (avregningsperiode.nyttBeløp - avregningsperiode.tidligereBeløp < BigDecimal.ZERO) BigDecimal(
                    -1
                ) else BigDecimal(1),
                avregningForrigeBeloep = avregningsperiode.tidligereBeløp,
                avregningNyttBeloep = avregningsperiode.nyttBeløp,
                belop = avregningsperiode.nyttBeløp - avregningsperiode.tidligereBeløp,
            )

        // status settes til BESTILT hvis det ikke er endring i hva som skal betales for gjeldende faktura og dermed ikke
        // skal sendes til OEBS. Grunnen til at vi oppretter en faktura er for å koble tidligere faktura med en ny avregning
        return Faktura(
            referanseNr = ULID.randomULID(),
            krediteringFakturaRef = avregningsperiode.opprinneligFaktura.referanseNr,
            fakturaLinje = listOf(fakturaLinje),
            status = if (avregningsperiode.nyttBeløp.compareTo(avregningsperiode.tidligereBeløp) != 0) FakturaStatus.OPPRETTET else FakturaStatus.BESTILT,
            referertFakturaVedAvregning = avregningsperiode.bestilteFaktura
        )
    }

    companion object {
        private val decimalFormat = DecimalFormat("0.00", DecimalFormatSymbols(Locale("no", "NO", "nb")))
        private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

        fun genererBeskrivelse(
            periodeFra: LocalDate,
            periodeTil: LocalDate,
            nyttBeløp: BigDecimal,
            tidligereBeløp: BigDecimal
        ): String {
            return "Periode: ${periodeFra.format(dateFormat)} - ${periodeTil.format(dateFormat)}\n" +
                    "Nytt beløp: ${decimalFormat.format(nyttBeløp)} - " +
                    "tidligere beløp: ${decimalFormat.format(tidligereBeløp)}"
        }
    }
}