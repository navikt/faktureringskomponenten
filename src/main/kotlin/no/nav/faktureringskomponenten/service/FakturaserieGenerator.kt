package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import no.nav.faktureringskomponenten.service.PeriodiseringUtil.substract
import no.nav.faktureringskomponenten.service.avregning.AvregningBehandler
import org.springframework.stereotype.Component
import org.threeten.extra.LocalDateRange
import java.time.LocalDate

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
        val fakturerbarePerioderPerIntervall = PeriodiseringUtil.delIFakturerbarePerioder(fakturaserieDto.perioder, fakturaserieDto.intervall)

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

}
