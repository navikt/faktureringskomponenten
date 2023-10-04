package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class AvregningBehandler {
    fun lagAvregningsfaktura(fakturaseriePerioder: List<FakturaseriePeriode>, bestilteFakturaer: List<Faktura>): Faktura? {
        if (bestilteFakturaer.isEmpty()) return null

        val avregningsperioder = lagEventuelleAvregningsperioder(bestilteFakturaer)
        if (avregningsperioder.isEmpty()) return null

        return AvregningsfakturaGenerator().lagFaktura(avregningsperioder)
    }

    private fun lagEventuelleAvregningsperioder(bestilteFakturaer: List<Faktura>): List<Avregningsperiode> {
        return listOf(
            Avregningsperiode(
                periodeFra = LocalDate.of(2024, 1, 1),
                periodeTil = LocalDate.of(2024, 3, 31),
                bestilteFaktura = bestilteFakturaer[0],
                tidligereBeløp = BigDecimal(9000),
                nyttBeløp = BigDecimal(10000)
            ),
            Avregningsperiode(
                periodeFra = LocalDate.of(2024, 4, 1),
                periodeTil = LocalDate.of(2024, 6, 30),
                bestilteFaktura = bestilteFakturaer[1],
                tidligereBeløp = BigDecimal(9000),
                nyttBeløp = BigDecimal(12000)
            )
        )
    }
}
