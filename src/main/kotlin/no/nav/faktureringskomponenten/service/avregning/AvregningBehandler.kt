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
        nyeFakturaseriePerioder: List<FakturaseriePeriode>,
        bestilteFakturaerFraForrigeFakturaserie: List<Faktura>
    ): List<Faktura> {
        if (bestilteFakturaerFraForrigeFakturaserie.isEmpty()) return emptyList()
        log.debug { "Lager avregningsfaktura for fakturaseriePerioder: $nyeFakturaseriePerioder" }
        log.debug { "Bestilte fakturaer: $bestilteFakturaerFraForrigeFakturaserie" }
        if (log.isDebugEnabled) {
            bestilteFakturaerFraForrigeFakturaserie.sortedBy { it.getPeriodeFra() }
                .forEachIndexed { index, linje -> log.debug { "Faktura ${index + 1} " + linje.getLinesAsString() } }
        }

        val avregningsperioder = finnAvregningsperioder(bestilteFakturaerFraForrigeFakturaserie, nyeFakturaseriePerioder)
        log.debug { "Avregningsperioder generert: $avregningsperioder" }

        return avregningsperioder.map {
            avregningsfakturaGenerator.lagFaktura(it)
        }.toList()
    }

    // Det skilles mellom bestilte fakturaer som overlapper med nye fakturaserieperioder og dermed skal avregnes
    // og bestilte fakturarer som ikke overlapper med nye fakturaserieperioder og skal nulles ut.
    private fun finnAvregningsperioder(
        bestilteFakturaerFraForrigeSerie: List<Faktura>,
        nyeFakturaseriePerioder: List<FakturaseriePeriode>
    ): List<Avregningsperiode> {
        val avregningsperioderForAvregningsfakturaerSomOverlapper =
            finnAvregningsfakturaerSomAvregnes(bestilteFakturaerFraForrigeSerie, nyeFakturaseriePerioder).map(::lagAvregningsperiode)
        val avregningsperioderForVanligeFakturaerSomOverlapper =
            finnVanligeFakturaerSomAvregnes(bestilteFakturaerFraForrigeSerie, nyeFakturaseriePerioder).map(::lagAvregningsperiode)
        val avregningsperioderForFakturaerSomIkkeOverlapper =
            finnFakturaerSomIkkeOverlapper(bestilteFakturaerFraForrigeSerie, nyeFakturaseriePerioder).filter { faktura ->
                sumAvregningerRekursivt(faktura).compareTo(BigDecimal.ZERO) != 0
            }.map(::lagAvregningsperiodeSomNullesUt)
        return (avregningsperioderForAvregningsfakturaerSomOverlapper + avregningsperioderForVanligeFakturaerSomOverlapper + avregningsperioderForFakturaerSomIkkeOverlapper)
    }

    /**
     * Finner alle tidligere bestilte fakturaer som ikke har perioder som overlapper med en eller flere perioder i den nye fakturaserien.
     *
     * @param bestilteFakturaer Fakturaer som ble tidligere bestilt i forrige fakturaserie
     * @param fakturaseriePerioder Perioder i den nye fakturaserien
     * @return Liste med relevante fakturaer
     */
    private fun finnFakturaerSomIkkeOverlapper(
        bestilteFakturaer: List<Faktura>, fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<Faktura> = bestilteFakturaer.flatMap { faktura ->
        val overlappendePerioder = overlappendeFakturaseriePerioder(fakturaseriePerioder, faktura.getPeriodeFra(), faktura.getPeriodeTil())
        if (overlappendePerioder.isEmpty()) listOf(faktura) else emptyList()
    }

    private fun lagAvregningsperiodeSomNullesUt(faktura: Faktura): Avregningsperiode {
        val sumAvregninger = sumAvregningerRekursivt(faktura)
        return Avregningsperiode(
            periodeFra = faktura.getPeriodeFra(),
            periodeTil = faktura.getPeriodeTil(),
            bestilteFaktura = faktura,
            opprinneligFaktura = hentFørstePositiveFaktura(faktura),
            tidligereBeløp = sumAvregninger,
            nyttBeløp = BigDecimal.ZERO
        )
    }

    private fun sumAvregningerRekursivt(faktura: Faktura): BigDecimal {
        var sum = if (faktura.erBestilt()) faktura.totalbeløp() else BigDecimal.ZERO

        faktura.referertFakturaVedAvregning?.let { referertFaktura ->
            sum += sumAvregningerRekursivt(referertFaktura)
        }

        return sum
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
     * @return Liste med tidligere faktura + linje + ny periode
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
     * @return Liste med tidligere faktura + ny periode
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
            tidligereBeløp = sumAvregningerRekursivt(faktura),
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