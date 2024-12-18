package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

object FakturaIntervallPeriodisering {
    fun genererPeriodisering(
        startDatoForPerioden: LocalDate,
        sluttDatoForPerioden: LocalDate,
        faktureringsintervall: FakturaserieIntervall
    ): List<Pair<LocalDate, LocalDate>> = generateSequence(startDatoForPerioden) { startDato ->
        sluttDatoFor(startDato, faktureringsintervall).plusDays(1)
    }.takeWhile { it <= sluttDatoForPerioden }
        .map { startDato ->
            val sluttDato = minOf(sluttDatoFor(startDato, faktureringsintervall), sluttDatoForPerioden)
            startDato to sluttDato
        }.toList()

    private fun sluttDatoFor(startDato: LocalDate, intervall: FakturaserieIntervall): LocalDate = when (intervall) {
        FakturaserieIntervall.MANEDLIG -> startDato.withDayOfMonth(startDato.lengthOfMonth())
        FakturaserieIntervall.KVARTAL -> startDato.withMonth(startDato[IsoFields.QUARTER_OF_YEAR] * 3).with(TemporalAdjusters.lastDayOfMonth())
        FakturaserieIntervall.SINGEL -> throw IllegalArgumentException("Singelintervall er ikke støttet")
    }
}
