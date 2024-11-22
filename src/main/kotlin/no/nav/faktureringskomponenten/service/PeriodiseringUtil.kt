package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.threeten.extra.LocalDateRange
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object PeriodiseringUtil {
    private fun YearMonth.getQuarterStart(): YearMonth {
        val monthInQuarter = (monthValue - 1) % 3
        return minusMonths(monthInQuarter.toLong())
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
        }.distinct()
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

    private fun FakturaserieIntervall.toMonthStep() = when (this) {
        FakturaserieIntervall.MANEDLIG -> 1L
        FakturaserieIntervall.KVARTAL -> 3L
        FakturaserieIntervall.SINGEL -> SingleErIkkeStøttet()
    }

    private fun SingleErIkkeStøttet(): Nothing {
        throw IllegalArgumentException("Singelintervall er ikke støttet")
    }
}