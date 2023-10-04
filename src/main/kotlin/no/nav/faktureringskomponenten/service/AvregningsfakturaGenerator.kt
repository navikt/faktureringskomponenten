package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import java.math.BigDecimal

class AvregningsfakturaGenerator {
    fun lagFaktura(avregningsperioder: List<Avregningsperiode>): Faktura? {
        if (avregningsperioder.isEmpty()) return null

        val fakturaLinjer = avregningsperioder.map {
            FakturaLinje(
                id = null,
                referertFakturaVedAvregning = it.bestilteFaktura,
                periodeFra = it.periodeFra,
                periodeTil = it.periodeTil,
                beskrivelse = "nytt beløp: ${it.nyttBeløp} - tidligere beløp: ${it.tidligereBeløp}",
                antall = BigDecimal(1),
                enhetsprisPerManed = it.nyttBeløp.minus(it.tidligereBeløp),
                belop = it.nyttBeløp.minus(it.tidligereBeløp),
            )
        }
        return Faktura(fakturaLinje = fakturaLinjer)
    }
}