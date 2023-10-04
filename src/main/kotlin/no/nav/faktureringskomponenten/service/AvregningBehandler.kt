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

        val avregningsperioder = lagEventuelleAvregningsperioder(bestilteFakturaer, fakturaseriePerioder)
        if (avregningsperioder.isEmpty()) return null

        return AvregningsfakturaGenerator().lagFaktura(avregningsperioder)
    }

    private fun lagEventuelleAvregningsperioder(
        bestilteFakturaer: List<Faktura>,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<Avregningsperiode> {
        // TODO bestilteFakturaerSomTrengerAvregning
        return bestilteFakturaer.map { Avregningsperiode(
            periodeFra = it.getPeriodeFra(),
            periodeTil = it.getPeriodeTil(),
            bestilteFaktura = it,
            tidligereBeløp = it.totalbeløp(),
            nyttBeløp = nyttBeløp(it.getPeriodeFra(), it.getPeriodeTil(), fakturaseriePerioder)
        )
        }
    }

    private fun nyttBeløp(periodeFra: LocalDate, periodeTil: LocalDate, fakturaseriePerioder: List<FakturaseriePeriode>): BigDecimal {
        if (periodeFra == LocalDate.of(2024, 1, 1)) return BigDecimal(10000)
        if (periodeFra == LocalDate.of(2024, 4, 1)) return BigDecimal(12000)
        return BigDecimal(1111)

    }
}
