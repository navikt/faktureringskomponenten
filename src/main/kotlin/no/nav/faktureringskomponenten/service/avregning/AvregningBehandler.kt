package no.nav.faktureringskomponenten.service.avregning

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.service.beregning.BeløpBeregner
import org.springframework.stereotype.Component
import org.threeten.extra.LocalDateRange
import java.math.BigDecimal
import java.time.LocalDate
import java.util.regex.Pattern

private data class AvregningsfakturaLinjeOgNyePerioder(val faktura: Faktura, val fakturaLinje: FakturaLinje, val nyePerioder: List<FakturaseriePeriode>)
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
        val avregningsperioderForTidligereAvregningsfaktura = finnAvregningsfakturaerSomAvregnes(bestilteFakturaer, fakturaseriePerioder).map(::lagAvregningsperiode)
        val avregningsperioderForVanligeFakturaer = finnVanligeFakturaerSomAvregnes(bestilteFakturaer, fakturaseriePerioder).map(::lagAvregningsperiode)
        return (avregningsperioderForTidligereAvregningsfaktura + avregningsperioderForVanligeFakturaer).filter { it.nyttBeløp.compareTo(it.tidligereBeløp) != 0 }
    }

    private fun finnAvregningsfakturaerSomAvregnes(bestilteFakturaer: List<Faktura>, fakturaseriePerioder: List<FakturaseriePeriode>): List<AvregningsfakturaLinjeOgNyePerioder> {
        return bestilteFakturaer.filter { it.erAvregningsfaktura() }
            .flatMap { faktura -> faktura.fakturaLinje.map { linje -> Pair(faktura, linje) } }
            .mapNotNull { (faktura, linje) ->
                val overlappendePerioder = overlappendeFakturaseriePerioder(fakturaseriePerioder, linje.periodeFra, linje.periodeTil)
                if (overlappendePerioder.isNotEmpty()) AvregningsfakturaLinjeOgNyePerioder(faktura, linje, overlappendePerioder) else null
            }
    }

    private fun finnVanligeFakturaerSomAvregnes(bestilteFakturaer: List<Faktura>, fakturaseriePerioder: List<FakturaseriePeriode>): List<FakturaOgNyePerioder> {
        return bestilteFakturaer.filter { !it.erAvregningsfaktura() }
            .mapNotNull {
                val overlappendePerioder = overlappendeFakturaseriePerioder(fakturaseriePerioder, it.getPeriodeFra(), it.getPeriodeTil())
                if (overlappendePerioder.isNotEmpty()) FakturaOgNyePerioder(it, overlappendePerioder) else null
            }
    }

    private fun overlappendeFakturaseriePerioder(fakturaseriePerioder: List<FakturaseriePeriode>, fom: LocalDate, tom: LocalDate): List<FakturaseriePeriode> {
        val bestilteFakturaPeriode = LocalDateRange.ofClosed(fom, tom)
        return fakturaseriePerioder.mapNotNull { periode ->
            LocalDateRange.ofClosed(periode.startDato, periode.sluttDato).takeIf { it.overlaps(bestilteFakturaPeriode) }?.let { periode }
        }
    }

    private fun lagAvregningsperiode(avregningsfakturaLinjeOgNyePerioder: AvregningsfakturaLinjeOgNyePerioder): Avregningsperiode {
        val (faktura, linje, overlappendePerioder) = avregningsfakturaLinjeOgNyePerioder
        val nyttBeløp = beregnNyttBeløp(overlappendePerioder, linje.periodeFra, linje.periodeTil)
        return Avregningsperiode(
            periodeFra = linje.periodeFra,
            periodeTil = linje.periodeTil,
            bestilteFaktura = faktura,
            tidligereBeløp = parseLinjeForTidligereBeløp(linje),
            nyttBeløp = nyttBeløp,
        )
    }

    private fun parseLinjeForTidligereBeløp(linje: FakturaLinje): BigDecimal {
        val matcher = Pattern.compile("-?\\d+(\\.\\d+)?").matcher(linje.beskrivelse)

        return if (matcher.find()) {
            BigDecimal(matcher.group())
        } else {
            throw RuntimeException("Tidligere beløp kunne ikke leses ved avregning for fakturalinje" + linje.id)
        }
    }

    private fun lagAvregningsperiode(fakturaOgNyePerioder: FakturaOgNyePerioder): Avregningsperiode {
        val (faktura, overlappendePerioder) = fakturaOgNyePerioder
        val nyttBeløp = beregnNyttBeløp(overlappendePerioder, faktura.getPeriodeFra(), faktura.getPeriodeTil())
        return Avregningsperiode(
            periodeFra = faktura.getPeriodeFra(),
            periodeTil = faktura.getPeriodeTil(),
            bestilteFaktura = faktura,
            tidligereBeløp = faktura.totalbeløp(),
            nyttBeløp = nyttBeløp,
        )
    }

    private fun beregnNyttBeløp(overlappendePerioder: List<FakturaseriePeriode>, fom: LocalDate, tom: LocalDate): BigDecimal {
        return overlappendePerioder.sumOf { periode -> beregnBeløpForEnkelPeriode(periode, fom, tom)}
    }

    private fun beregnBeløpForEnkelPeriode(fakturaseriePeriode: FakturaseriePeriode, fom: LocalDate, tom: LocalDate): BigDecimal {
        val fakturaDateRange = LocalDateRange.ofClosed(fom, tom)
        val periodeDateRange = LocalDateRange.ofClosed(fakturaseriePeriode.startDato, fakturaseriePeriode.sluttDato)
        val overlappDateRange = fakturaDateRange.intersection(periodeDateRange)
        return BeløpBeregner.beløpForPeriode(fakturaseriePeriode.enhetsprisPerManed, overlappDateRange.start, overlappDateRange.endInclusive)
    }
}