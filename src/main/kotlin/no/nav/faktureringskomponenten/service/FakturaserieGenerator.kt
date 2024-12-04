package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.service.avregning.AvregningBehandler
import org.springframework.stereotype.Component
import org.threeten.extra.LocalDateRange
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Component
class FakturaserieGenerator(
    val fakturaGenerator: FakturaGenerator,
    val avregningBehandler: AvregningBehandler
) {

    fun lagFakturaserie(
        fakturaserieDto: FakturaserieDto,
        startDato: LocalDate? = null,
        avregningsfaktura: List<Faktura> = emptyList()
    ): Fakturaserie {
        val avregningsfakturaSistePeriodeTil = avregningsfaktura.maxByOrNull { it.getPeriodeTil() }?.getPeriodeTil()
        val startDatoForSamletPeriode =
            finnStartDatoForSamletPeriode(avregningsfakturaSistePeriodeTil, startDato, fakturaserieDto)
        val sluttDatoForSamletPeriode = fakturaserieDto.perioder.maxBy { it.sluttDato }.sluttDato

        val fakturaerForSamletPeriode = fakturaGenerator.lagFakturaerFor(
            startDatoForSamletPeriode,
            sluttDatoForSamletPeriode,
            fakturaserieDto.perioder,
            fakturaserieDto.intervall
        )
        return Fakturaserie(
            id = null,
            referanse = fakturaserieDto.fakturaserieReferanse,
            fakturaGjelderInnbetalingstype = fakturaserieDto.fakturaGjelderInnbetalingstype,
            fodselsnummer = fakturaserieDto.fodselsnummer,
            fullmektig = mapFullmektig(fakturaserieDto.fullmektig),
            referanseBruker = fakturaserieDto.referanseBruker,
            referanseNAV = fakturaserieDto.referanseNAV,
            startdato = startDatoForSamletPeriode,
            sluttdato = sluttDatoForSamletPeriode,
            intervall = fakturaserieDto.intervall,
            faktura = fakturaerForSamletPeriode + avregningsfaktura
        )
    }

    fun lagFakturaserieForEndring(
        fakturaserieDto: FakturaserieDto,
        opprinneligFakturaserie: Fakturaserie
    ): Fakturaserie {
        val startDato = finnStartDatoForFørstePlanlagtFaktura(opprinneligFakturaserie)
        val avregningsfakturaer = avregningBehandler.lagAvregningsfakturaer(
            fakturaserieDto.perioder,
            opprinneligFakturaserie.bestilteFakturaer()
        )
        val nyeFakturaerForNyePerioder: List<Faktura> = lagNyeFakturaerForNyePerioder(fakturaserieDto, avregningsfakturaer)

        return lagFakturaserie(fakturaserieDto, startDato, avregningsfakturaer + nyeFakturaerForNyePerioder)
    }

    private fun finnStartDatoForFørstePlanlagtFaktura(opprinneligFakturaserie: Fakturaserie) =
        if (opprinneligFakturaserie.erUnderBestilling()) {
            opprinneligFakturaserie.planlagteFakturaer().minByOrNull { it.getPeriodeFra() }?.getPeriodeFra()
        } else null

    private fun lagNyeFakturaerForNyePerioder(
        fakturaserieDto: FakturaserieDto,
        avregningsfakturaer: List<Faktura>
    ): List<Faktura> {
        val fakturerbarePerioderPerIntervall = delIFakturerbarePerioder(fakturaserieDto.perioder, fakturaserieDto.intervall)

        val nyeFakturaPerioder = fakturerbarePerioderPerIntervall.flatMap { periode ->
            val avregningsperioder = avregningsfakturaer.map { LocalDateRange.of(it.getPeriodeFra(), it.getPeriodeTil()) }
            if (avregningsperioder.none { it.overlaps(periode) }) listOf(periode)
            else avregningsperioder.filter { it.overlaps(periode) && !it.encloses(periode) }.flatMap { periode.substract(it) }
        }
        val nyeFakturaerForNyePerioder: List<Faktura> = nyeFakturaPerioder.map {
            val perioder = fakturaserieDto.perioder.filter { periode -> LocalDateRange.of(periode.startDato, periode.sluttDato).overlaps(it) }
            fakturaGenerator.lagFaktura(it.start, it.end, perioder)
        }
        return nyeFakturaerForNyePerioder
    }

    fun lagFakturaserieForKansellering(
        fakturaserieDto: FakturaserieDto,
        startDato: LocalDate,
        sluttDato: LocalDate,
        bestilteFakturaer: List<Faktura>
    ): Fakturaserie {
        return Fakturaserie(
            referanse = fakturaserieDto.fakturaserieReferanse,
            fakturaGjelderInnbetalingstype = fakturaserieDto.fakturaGjelderInnbetalingstype,
            fodselsnummer = fakturaserieDto.fodselsnummer,
            fullmektig = mapFullmektig(fakturaserieDto.fullmektig),
            referanseBruker = fakturaserieDto.referanseBruker,
            referanseNAV = fakturaserieDto.referanseNAV,
            startdato = startDato,
            sluttdato = sluttDato,
            intervall = fakturaserieDto.intervall,
            faktura = avregningBehandler.lagAvregningsfakturaer(
                fakturaserieDto.perioder,
                bestilteFakturaer
            )
        )
    }

    private fun finnStartDatoForSamletPeriode(
        avregningsfakturaSistePeriodeTil: LocalDate?,
        startDato: LocalDate?,
        fakturaserieDto: FakturaserieDto
    ) = avregningsfakturaSistePeriodeTil?.plusDays(1) ?: startDato ?: fakturaserieDto.perioder.minBy { it.startDato }.startDato

    private fun mapFullmektig(fullmektigDto: Fullmektig?): Fullmektig? {
        if (fullmektigDto != null) {
            return Fullmektig(
                fodselsnummer = fullmektigDto.fodselsnummer,
                organisasjonsnummer = fullmektigDto.organisasjonsnummer,
            )
        }
        return null
    }

    companion object {
        private fun delIFakturerbarePerioder(
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
            }.distinct()
        }

        private fun FakturaserieIntervall.toMonthStep() = when (this) {
            FakturaserieIntervall.MANEDLIG -> 1L
            FakturaserieIntervall.KVARTAL -> 3L
            FakturaserieIntervall.SINGEL -> SingleErIkkeStøttet()
        }

        private fun YearMonth.toDateRange(
            startConstraint: LocalDate,
            endConstraint: LocalDate,
            intervall: FakturaserieIntervall
        ): LocalDateRange {
            val periodStart = when (intervall) {
                FakturaserieIntervall.MANEDLIG -> maxOf(this.atDay(1), startConstraint)
                FakturaserieIntervall.KVARTAL -> maxOf(this.getQuarterStart().atDay(1), startConstraint)
                FakturaserieIntervall.SINGEL -> SingleErIkkeStøttet()
            }

            val periodEnd = when (intervall) {
                FakturaserieIntervall.MANEDLIG -> minOf(this.atEndOfMonth(), endConstraint)
                FakturaserieIntervall.KVARTAL -> minOf(this.getQuarterStart().plusMonths(2).atEndOfMonth(), endConstraint)
                FakturaserieIntervall.SINGEL -> SingleErIkkeStøttet()
            }

            return LocalDateRange.of(periodStart, periodEnd)
        }

        private fun YearMonth.getQuarterStart(): YearMonth {
            val monthInQuarter = (monthValue - 1) % 3
            return minusMonths(monthInQuarter.toLong())
        }

        private fun SingleErIkkeStøttet(): Nothing {
            throw IllegalArgumentException("Singelintervall er ikke støttet")
        }

        fun LocalDateRange.substract(other: LocalDateRange): List<LocalDateRange> {
            if (!isConnected(other)) return listOf(this)
            return buildList {
                if (start < other.start) {
                    add(LocalDateRange.of(start, other.start.minusDays(1)))
                }
                if (end > other.end) {
                    add(LocalDateRange.of(other.end.plusDays(1), end))
                }
            }
        }
    }
}
