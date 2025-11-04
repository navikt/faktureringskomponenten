package no.nav.faktureringskomponenten.service

import io.getunleash.Unleash
import mu.KotlinLogging
import no.nav.faktureringskomponenten.config.ToggleName
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import no.nav.faktureringskomponenten.service.avregning.AvregningBehandler
import org.springframework.stereotype.Component
import org.threeten.extra.LocalDateRange
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Component
class FakturaserieGenerator(
    val fakturaGenerator: FakturaGenerator,
    val avregningBehandler: AvregningBehandler,
    val unleash: Unleash
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

        if (startDato != null && startDato.isAfter(sluttDatoForSamletPeriode))
            log.error(
                "startDato er etter sluttdato. AvregningsfakturaSistePeriodeTil: $avregningsfakturaSistePeriodeTil" +
                    " startDato: $startDato startDatoForSamletPeriode $startDatoForSamletPeriode sluttDatoForSamletPeriode $sluttDatoForSamletPeriode" +
                    " fakturaserieDto: $fakturaserieDto"
            )

        val periodisering =
            FakturaIntervallPeriodisering.genererPeriodisering(startDatoForSamletPeriode, sluttDatoForSamletPeriode, fakturaserieDto.intervall)
        val fakturaerForSamletPeriode = fakturaGenerator.lagFakturaerFor(periodisering, fakturaserieDto.perioder, fakturaserieDto.intervall)

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
        val fakturaSomSkalBrukesIAvregning = if (unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
            opprinneligFakturaserie.bestilteFakturaer().filter { it.alleFakturaLinjerErFraIÅrEllerFremover() }
        } else {
            opprinneligFakturaserie.bestilteFakturaer()
        }
        val avregningsfakturaer = avregningBehandler.lagAvregningsfakturaer(
            fakturaserieDto.perioder,
            fakturaSomSkalBrukesIAvregning
        )

        if (unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
            if (fakturaserieDto.perioder.isEmpty()) {
                if (avregningsfakturaer.isEmpty()) {
                    error("Kan ikke opprette fakturaserie med tomme perioder og ingen avregningsfakturaer. FakturaserieDto: $fakturaserieDto")
                }
                return Fakturaserie(
                    id = null,
                    referanse = fakturaserieDto.fakturaserieReferanse,
                    fakturaGjelderInnbetalingstype = fakturaserieDto.fakturaGjelderInnbetalingstype,
                    fodselsnummer = fakturaserieDto.fodselsnummer,
                    fullmektig = mapFullmektig(fakturaserieDto.fullmektig),
                    referanseBruker = fakturaserieDto.referanseBruker,
                    referanseNAV = fakturaserieDto.referanseNAV,
                    startdato = avregningsfakturaer.minBy { it.getPeriodeFra() }.getPeriodeFra(),
                    sluttdato = avregningsfakturaer.maxBy { it.getPeriodeTil() }.getPeriodeTil(),
                    intervall = fakturaserieDto.intervall,
                    faktura = avregningsfakturaer
                )
            }
        }

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
        val fellesPeriodisering = FakturaIntervallPeriodisering.genererPeriodisering(
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
            fakturaserieDto.perioder,
            fakturaserieDto.intervall
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
