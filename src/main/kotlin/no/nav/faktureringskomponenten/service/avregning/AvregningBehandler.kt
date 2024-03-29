package no.nav.faktureringskomponenten.service.avregning

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.service.beregning.BeløpBeregner
import org.springframework.stereotype.Component
import org.threeten.extra.LocalDateRange
import java.math.BigDecimal
import java.time.LocalDate

private val log = KotlinLogging.logger { }

private data class AvregningsfakturaLinjeOgNyePerioder(
    val faktura: Faktura,
    val fakturaLinje: FakturaLinje,
    val nyePerioder: List<FakturaseriePeriode>
)

private data class FakturaOgNyePerioder(val faktura: Faktura, val nyePerioder: List<FakturaseriePeriode>)

@Component
class AvregningBehandler(private val avregningsfakturaGenerator: AvregningsfakturaGenerator) {
    fun lagAvregningsfaktura(
        fakturaseriePerioder: List<FakturaseriePeriode>,
        bestilteFakturaer: List<Faktura>
    ): List<Faktura> {
        if (bestilteFakturaer.isEmpty()) return emptyList()
        log.debug { "Lager avregningsfaktura for fakturaseriePerioder: $fakturaseriePerioder" }
        log.debug { "Bestilte fakturaer: $bestilteFakturaer" }
        if (log.isDebugEnabled) {
            bestilteFakturaer.sortedBy { it.getPeriodeFra() }
                .forEachIndexed { index, linje -> log.debug { "Faktura ${index + 1} " + linje.getLinesAsString() } }
        }

        val avregningsperioder = lagEventuelleAvregningsperioder(bestilteFakturaer, fakturaseriePerioder)
        if (avregningsperioder.isEmpty()) return emptyList()
        log.debug { "Avregningsperioder generert: $avregningsperioder" }

        return avregningsperioder.map {
            avregningsfakturaGenerator.lagFaktura(it)
        }.toList()
    }

    private fun lagEventuelleAvregningsperioder(
        bestilteFakturaer: List<Faktura>,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<Avregningsperiode> {
        val avregningsperioderForTidligereAvregningsfaktura =
            finnAvregningsfakturaerSomAvregnes(bestilteFakturaer, fakturaseriePerioder).map(::lagAvregningsperiode)
        val avregningsperioderForVanligeFakturaer =
            finnVanligeFakturaerSomAvregnes(bestilteFakturaer, fakturaseriePerioder).map(::lagAvregningsperiode)
        val avregningsperioderForPerioderSomIkkeOverlapper =
            finnPerioderSomIkkeOverlapper(bestilteFakturaer, fakturaseriePerioder)
        return (avregningsperioderForTidligereAvregningsfaktura + avregningsperioderForVanligeFakturaer + avregningsperioderForPerioderSomIkkeOverlapper)
    }

    /**
     * Finner alle fakturaer som ikke er avregningsfakturaer og som ikke har perioder som overlapper med en eller flere perioder i fakturaserien.
     * Dersom nye perioder ikke overlapper med eksisterende bestilte faktura, lages det en Avregningsperiode perioden.
     *
     * @param bestilteFakturaer Fakturaer som er bestilt
     * @param fakturaseriePerioder Perioder i fakturaserien
     * @return Liste med Avregningsperiode
     */
    private fun finnPerioderSomIkkeOverlapper(
        bestilteFakturaer: List<Faktura>,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<Avregningsperiode> {
        val perioderSomIkkeOverlapper = bestilteFakturaer.flatMap { faktura ->
            val overlappendePerioder =
                overlappendeFakturaseriePerioder(fakturaseriePerioder, faktura.getPeriodeFra(), faktura.getPeriodeTil())
            if (overlappendePerioder.isEmpty()) listOf(faktura) else emptyList()
        }
        return perioderSomIkkeOverlapper.map { faktura ->
            Avregningsperiode(
                periodeFra = faktura.getPeriodeFra(),
                periodeTil = faktura.getPeriodeTil(),
                bestilteFaktura = faktura,
                opprinneligFaktura = hentFørstePositiveFaktura(faktura),
                tidligereBeløp = faktura.totalbeløp(),
                nyttBeløp = BigDecimal.ZERO
            )
        }
    }

    /**
     * Finner alle fakturaer som er avregningsfakturaer og som har linjer som overlapper med en eller flere perioder i fakturaserien.
     * For hver linje som overlapper med en eller flere perioder i fakturaserien, lages en Avregningsperiode.
     * Hvis en linje overlapper med flere perioder, lages det en Avregningsperiode for hver periode.
     * Hvis en linje overlapper med en periode, lages det en Avregningsperiode for denne perioden.
     * Hvis en linje overlapper med ingen perioder, lages det ingen Avregningsperiode.
     *
     * @param bestilteFakturaer Fakturaer som er bestilt
     * @param fakturaseriePerioder Perioder i fakturaserien
     * @return Liste med Avregningsperiode
     */
    private fun finnAvregningsfakturaerSomAvregnes(
        bestilteFakturaer: List<Faktura>,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<AvregningsfakturaLinjeOgNyePerioder> {
        return bestilteFakturaer.filter { it.erAvregningsfaktura() }
            .flatMap { faktura -> faktura.fakturaLinje.map { linje -> Pair(faktura, linje) } }
            .mapNotNull { (faktura, linje) ->
                val overlappendePerioder =
                    overlappendeFakturaseriePerioder(fakturaseriePerioder, linje.periodeFra, linje.periodeTil)
                if (overlappendePerioder.isNotEmpty()) AvregningsfakturaLinjeOgNyePerioder(
                    faktura,
                    linje,
                    overlappendePerioder
                ) else null
            }
    }

    /**
     * Finner alle fakturaer som ikke er avregningsfakturaer og som har perioder som overlapper med en eller flere perioder i fakturaserien.
     * For hver faktura som overlapper med en eller flere perioder i fakturaserien, lages en Avregningsperiode.
     * Hvis en faktura overlapper med flere perioder, lages det en Avregningsperiode for hver periode.
     * Hvis en faktura overlapper med en periode, lages det en Avregningsperiode for denne perioden.
     * Hvis en faktura overlapper med ingen perioder, lages det ingen Avregningsperiode.
     *
     * @param bestilteFakturaer Fakturaer som er bestilt
     * @param fakturaseriePerioder Perioder i fakturaserien
     * @return Liste med Avregningsperiode
     */
    private fun finnVanligeFakturaerSomAvregnes(
        bestilteFakturaer: List<Faktura>,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<FakturaOgNyePerioder> {
        return bestilteFakturaer.filter { !it.erAvregningsfaktura() }
            .mapNotNull {
                val overlappendePerioder =
                    overlappendeFakturaseriePerioder(fakturaseriePerioder, it.getPeriodeFra(), it.getPeriodeTil())
                if (overlappendePerioder.isNotEmpty()) FakturaOgNyePerioder(it, overlappendePerioder) else null
            }
    }

    private fun overlappendeFakturaseriePerioder(
        fakturaseriePerioder: List<FakturaseriePeriode>,
        fom: LocalDate,
        tom: LocalDate
    ): List<FakturaseriePeriode> {
        val bestilteFakturaPeriode = LocalDateRange.ofClosed(fom, tom)
        return fakturaseriePerioder.mapNotNull { periode ->
            LocalDateRange.ofClosed(periode.startDato, periode.sluttDato).takeIf { it.overlaps(bestilteFakturaPeriode) }
                ?.let { periode }
        }
    }

    private fun lagAvregningsperiode(avregningsfakturaLinjeOgNyePerioder: AvregningsfakturaLinjeOgNyePerioder): Avregningsperiode {
        val (faktura, tidligereLinje, overlappendePerioder) = avregningsfakturaLinjeOgNyePerioder
        val nyttBeløp = beregnNyttBeløp(overlappendePerioder, tidligereLinje.periodeFra, tidligereLinje.periodeTil)
        return Avregningsperiode(
            periodeFra = tidligereLinje.periodeFra,
            periodeTil = tidligereLinje.periodeTil,
            bestilteFaktura = faktura,
            opprinneligFaktura = hentFørstePositiveFaktura(faktura),
            tidligereBeløp = tidligereLinje.avregningNyttBeloep!!,
            nyttBeløp = nyttBeløp,
        )
    }

    private fun lagAvregningsperiode(fakturaOgNyePerioder: FakturaOgNyePerioder): Avregningsperiode {
        val (faktura, overlappendePerioder) = fakturaOgNyePerioder
        val nyttBeløp = beregnNyttBeløp(overlappendePerioder, faktura.getPeriodeFra(), faktura.getPeriodeTil())
        return Avregningsperiode(
            periodeFra = faktura.getPeriodeFra(),
            periodeTil = faktura.getPeriodeTil(),
            bestilteFaktura = faktura,
            opprinneligFaktura = hentFørstePositiveFaktura(faktura),
            tidligereBeløp = faktura.totalbeløp(),
            nyttBeløp = nyttBeløp,
        )
    }

    private fun beregnNyttBeløp(
        overlappendePerioder: List<FakturaseriePeriode>,
        fom: LocalDate,
        tom: LocalDate
    ): BigDecimal {
        return overlappendePerioder.sumOf { periode -> beregnBeløpForEnkelPeriode(periode, fom, tom) }
    }

    private fun beregnBeløpForEnkelPeriode(
        fakturaseriePeriode: FakturaseriePeriode,
        fom: LocalDate,
        tom: LocalDate
    ): BigDecimal {
        val fakturaDateRange = LocalDateRange.ofClosed(fom, tom)
        val periodeDateRange = LocalDateRange.ofClosed(fakturaseriePeriode.startDato, fakturaseriePeriode.sluttDato)
        val overlappDateRange = fakturaDateRange.intersection(periodeDateRange)
        return BeløpBeregner.beløpForPeriode(
            fakturaseriePeriode.enhetsprisPerManed,
            overlappDateRange.start,
            overlappDateRange.endInclusive
        )
    }

    private fun hentFørstePositiveFaktura(faktura: Faktura): Faktura {
        if (faktura.totalbeløp() > BigDecimal.ZERO) {
            return faktura
        }
        return hentFørstePositiveFaktura(
            faktura.referertFakturaVedAvregning
                ?: throw RuntimeException("Faktura med referanse: ${faktura.referanseNr} mangler referertFakturaVedAvregning")
        )
    }
}