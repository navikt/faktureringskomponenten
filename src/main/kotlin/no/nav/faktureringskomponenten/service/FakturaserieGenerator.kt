package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import no.nav.faktureringskomponenten.service.avregning.AvregningBehandler
import org.springframework.stereotype.Component
import org.threeten.extra.LocalDateRange
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

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

        val periodisering = genererPeriodisering(startDatoForSamletPeriode, sluttDatoForSamletPeriode, fakturaserieDto.intervall)
        val fakturaerForSamletPeriode = fakturaGenerator.lagFakturaerFor(periodisering, fakturaserieDto.perioder)

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
        val fellesPeriodisering = genererPeriodisering(
            fakturaserieDto.perioder.minBy { it.startDato }.startDato,
            fakturaserieDto.perioder.maxBy { it.sluttDato }.sluttDato,
            fakturaserieDto.intervall
        ).map { LocalDateRange.of(it.first, it.second) }

        val periodiseringUtenAvregning = fellesPeriodisering.flatMap { periode ->
            val avregningsperioder = avregningsfakturaer.map { LocalDateRange.ofClosed(it.getPeriodeFra(), it.getPeriodeTil()) }
            if (avregningsperioder.none { it.overlaps(periode) }) listOf(periode)
            else avregningsperioder.filter { it.overlaps(periode) && !it.encloses(periode) }.flatMap { periode.substract(it) }
        }.map { Pair(it.start, it.end) }

        val nyeFakturaerForNyePerioder: List<Faktura> = fakturaGenerator.lagFakturaerFor(
            periodiseringUtenAvregning,
            fakturaserieDto.perioder
        )
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
            FakturaserieIntervall.SINGEL -> SingleErIkkeStøttet()
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
