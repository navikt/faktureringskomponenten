package no.nav.faktureringskomponenten.service.avregning

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.service.beregning.BeløpBeregner
import org.springframework.stereotype.Component
import org.threeten.extra.LocalDateRange
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger { }

private data class AvregningsfakturaLinjeOgNyePerioder(
    val faktura: Faktura,
    val fakturaLinje: FakturaLinje,
    val nyePerioder: List<FakturaseriePeriode>
)

private data class FakturaOgNyePerioder(val faktura: Faktura, val nyePerioder: List<FakturaseriePeriode>)

@Component
class AvregningBehandler(
    private val avregningsfakturaGenerator: AvregningsfakturaGenerator
) {

    fun lagAvregningsfakturaer(
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

        val avregningsperioder =
            finnAvregningsperioder(bestilteFakturaerFraForrigeFakturaserie, nyeFakturaseriePerioder)
        log.debug { "Avregningsperioder generert: $avregningsperioder" }

        return avregningsperioder.map {
            avregningsfakturaGenerator.lagFaktura(it)
        }.toList()
    }

    private fun YearMonth.getQuarterStart(): YearMonth {
        val monthInQuarter = (monthValue - 1) % 3
        return minusMonths(monthInQuarter.toLong())
    }

    private fun YearMonth.toDateRange(
        startConstraint: LocalDate,
        endConstraint: LocalDate,
        intervall: FakturaserieIntervall
    ): LocalDateRange {
        val periodStart = when (intervall) {
            FakturaserieIntervall.MANEDLIG -> maxOf(this.atDay(1), startConstraint)
            FakturaserieIntervall.KVARTAL -> maxOf(this.getQuarterStart().atDay(1), startConstraint)
            FakturaserieIntervall.SINGEL -> TODO()
        }

        val periodEnd = when (intervall) {
            FakturaserieIntervall.MANEDLIG -> minOf(this.atEndOfMonth(), endConstraint)
            FakturaserieIntervall.KVARTAL -> minOf(this.getQuarterStart().plusMonths(2).atEndOfMonth(), endConstraint)
            FakturaserieIntervall.SINGEL -> TODO()
        }

        return LocalDateRange.of(periodStart, periodEnd)
    }

    // These functions can remain unchanged
    private fun FakturaserieIntervall.toMonthStep() = when (this) {
        FakturaserieIntervall.MANEDLIG -> 1L
        FakturaserieIntervall.KVARTAL -> 3L
        FakturaserieIntervall.SINGEL -> TODO()
    }

    fun delIFakturerbarePerioder(
        nyeFakturaseriePerioder: List<FakturaseriePeriode>,
        intervall: FakturaserieIntervall
    ): List<LocalDateRange> {
        return nyeFakturaseriePerioder.flatMap { periode ->
            val startYearMonth = YearMonth.from(periode.startDato)
            val endYearMonth = YearMonth.from(periode.sluttDato)

            (0..startYearMonth.until(endYearMonth, ChronoUnit.MONTHS))
                .filter { it % intervall.toMonthStep() == 0L }
                .map { monthsToAdd ->
                    startYearMonth
                        .plusMonths(monthsToAdd)
                        .toDateRange(
                            periode.startDato,
                            periode.sluttDato,
                            intervall
                        )
                }
        }
    }

    // Det skilles mellom bestilte fakturaer som overlapper med nye fakturaserieperioder og dermed skal avregnes
    // og bestilte fakturarer som ikke overlapper med nye fakturaserieperioder og skal nulles ut.
    private fun finnAvregningsperioder(
        bestilteFakturaerFraForrigeSerie: List<Faktura>,
        nyeFakturaseriePerioder: List<FakturaseriePeriode>
    ): List<Avregningsperiode> {
        println("bestilteFakturaerFraForrigeSerien")
        bestilteFakturaerFraForrigeSerie.forEach {
            println("fom:${it.getPeriodeFra()} tom:${it.getPeriodeTil()} totalbeløp:${it.totalbeløp()}")
        }

        println("nyeFakturaseriePerioder")
        nyeFakturaseriePerioder.forEach {
            println("fom:${it.startDato} tom:${it.sluttDato} enhetspris:${it.enhetsprisPerManed}")
        }

        println("delIFakturerbarePerioder")
        val fakturerbarePerioder = delIFakturerbarePerioder(nyeFakturaseriePerioder, FakturaserieIntervall.KVARTAL)

        val avregningsperioderForAvregningsfakturaerSomOverlapper =
            finnAvregningsfakturaerSomAvregnes(
                bestilteFakturaerFraForrigeSerie,
                nyeFakturaseriePerioder
            ).map(::lagAvregningsperiode)

        val avregningsperioderForVanligeFakturaerSomOverlapper =
            finnVanligeFakturaerSomAvregnes(
                bestilteFakturaerFraForrigeSerie,
                nyeFakturaseriePerioder
            ).map(::lagAvregningsperiode)

        val avregningsperioderForFakturaerSomIkkeOverlapper =
            finnFakturaerSomIkkeOverlapper(
                bestilteFakturaerFraForrigeSerie,
                nyeFakturaseriePerioder
            ).filter { faktura ->
                sumAvregningerRekursivt(faktura).compareTo(BigDecimal.ZERO) != 0
            }.map(::lagAvregningsperiodeSomNullesUt)

        val avregningsperioder: List<Avregningsperiode> =
            avregningsperioderForAvregningsfakturaerSomOverlapper + avregningsperioderForVanligeFakturaerSomOverlapper + avregningsperioderForFakturaerSomIkkeOverlapper

        avregningsperioder.forEach {
            println("fom:${it.periodeFra} tom:${it.periodeTil} tidligereBeløp:${it.tidligereBeløp} nyttBeløp:${it.nyttBeløp}")
        }

        val nyeFakturaPerioder = fakturerbarePerioder.filter { faktuerbarPeriode ->
            avregningsperioder.none { faktuerbarPeriode.overlaps(LocalDateRange.of(it.periodeFra, it.periodeTil)) }
        }

        println("nyeFakturaPerioder")
        nyeFakturaPerioder.forEach {
            println("fom:${it.start} tom:${it.end}")
        }

        log.debug { "avregningsperioderForFakturaerSomIkkeOverlapper:$avregningsperioderForFakturaerSomIkkeOverlapper" }

        return avregningsperioder
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
        val overlappendePerioder =
            overlappendeFakturaseriePerioder(fakturaseriePerioder, faktura.getPeriodeFra(), faktura.getPeriodeTil())
        if (overlappendePerioder.isEmpty()) listOf(faktura) else emptyList()
    }

    private fun lagAvregningsperiodeSomNullesUt(faktura: Faktura): Avregningsperiode {
        val sumAvregninger = sumAvregningerRekursivt(faktura)
        return Avregningsperiode(
            periodeFra = faktura.getPeriodeFra(),
            periodeTil = faktura.getPeriodeTil(),
            bestilteFaktura = faktura,
            opprinneligFaktura = faktura.hentFørstePositiveFaktura(),
            tidligereBeløp = sumAvregninger,
            nyttBeløp = BigDecimal.ZERO
        )
    }

    private fun lagAvregningsperiodeForNyePerioder(fakturerbarePerioder: List<LocalDateRange>, nyttBelop: BigDecimal){
        return
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
            opprinneligFaktura = faktura.hentFørstePositiveFaktura(),
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
            opprinneligFaktura = faktura.hentFørstePositiveFaktura(),
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
}