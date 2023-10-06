package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.service.beregning.BeløpBeregner
import org.springframework.stereotype.Component
import org.threeten.extra.LocalDateRange
import java.math.BigDecimal

private data class FakturaOgNyePerioder(val faktura: Faktura, val nyePerioder: List<FakturaseriePeriode>)

@Component
class AvregningBehandler(private val avregningsfakturaGenerator: AvregningsfakturaGenerator) {
    fun lagAvregningsfaktura(fakturaseriePerioder: List<FakturaseriePeriode>, bestilteFakturaer: List<Faktura>): Faktura? {
        if (bestilteFakturaer.isEmpty()) return null

        val avregningsperioder = lagEventuelleAvregningsperioder(bestilteFakturaer, fakturaseriePerioder)
        if (avregningsperioder.isEmpty()) return null

        return avregningsfakturaGenerator.lagFaktura(avregningsperioder)
    }

    private fun lagEventuelleAvregningsperioder(
        bestilteFakturaer: List<Faktura>,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<Avregningsperiode> {
        val finnBestilteFakturaerSomAvregnes = finnBestilteFakturaerSomAvregnes(bestilteFakturaer, fakturaseriePerioder)
        return finnBestilteFakturaerSomAvregnes.map(::lagAvregningsperiode)
    }

    private fun finnBestilteFakturaerSomAvregnes(bestilteFakturaer: List<Faktura>, fakturaseriePerioder: List<FakturaseriePeriode>): List<FakturaOgNyePerioder> {
        return bestilteFakturaer.map { FakturaOgNyePerioder(it, overlappendeFakturaseriePerioder(fakturaseriePerioder, it)) }
    }

    private fun overlappendeFakturaseriePerioder(fakturaseriePerioder: List<FakturaseriePeriode>, faktura: Faktura): List<FakturaseriePeriode> {
        val bestilteFakturaPeriode = LocalDateRange.ofClosed(faktura.getPeriodeFra(), faktura.getPeriodeTil())
        return fakturaseriePerioder.mapNotNull { periode ->
            LocalDateRange.ofClosed(periode.startDato, periode.sluttDato).takeIf { it.overlaps(bestilteFakturaPeriode) }?.let { periode }
        }
    }

    private fun lagAvregningsperiode(fakturaOgNyePerioder: FakturaOgNyePerioder): Avregningsperiode {
        val (faktura, overlappendePerioder) = fakturaOgNyePerioder
        val nyttBeløp = beregnNyttBeløp(overlappendePerioder, faktura)
        return Avregningsperiode(
            periodeFra = faktura.getPeriodeFra(),
            periodeTil = faktura.getPeriodeTil(),
            bestilteFaktura = faktura,
            tidligereBeløp = faktura.totalbeløp(),
            nyttBeløp = nyttBeløp,
        )
    }

    private fun beregnNyttBeløp(overlappendePerioder: List<FakturaseriePeriode>, faktura: Faktura): BigDecimal {
        return overlappendePerioder.sumOf { periode -> beregnBeløpForEnkelPeriode(periode, faktura)}
    }

    private fun beregnBeløpForEnkelPeriode(fakturaseriePeriode: FakturaseriePeriode, faktura: Faktura): BigDecimal {
        val fakturaDateRange = LocalDateRange.ofClosed(faktura.getPeriodeFra(), faktura.getPeriodeTil())
        val periodeDateRange = LocalDateRange.ofClosed(fakturaseriePeriode.startDato, fakturaseriePeriode.sluttDato)
        val overlappDateRange = fakturaDateRange.intersection(periodeDateRange)
        return BeløpBeregner.beløpForPeriode(fakturaseriePeriode.enhetsprisPerManed, overlappDateRange.start, overlappDateRange.endInclusive)
    }
}
