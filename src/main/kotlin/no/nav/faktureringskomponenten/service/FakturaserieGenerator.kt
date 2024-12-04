package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.service.avregning.AvregningBehandler
import org.springframework.stereotype.Component
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
        val sluttDatoForSamletPeriode = mapSluttdato(fakturaserieDto.perioder)
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
        startDato: LocalDate? = null,
        avregningsfaktura: List<Faktura> = emptyList()
    ): Fakturaserie {
        return lagFakturaserie(fakturaserieDto, startDato, avregningsfaktura)
    }

    fun lagFakturaserieForKansellering(
        fakturaserieDto: FakturaserieDto,
        startDato: LocalDate,
        sluttDato: LocalDate,
        avregningsfaktura: List<Faktura>
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
            faktura = avregningsfaktura
        )
    }

    private fun finnStartDatoForSamletPeriode(
        avregningsfakturaSistePeriodeTil: LocalDate?,
        startDato: LocalDate?,
        fakturaserieDto: FakturaserieDto
    ) = avregningsfakturaSistePeriodeTil?.plusDays(1) ?: startDato ?: mapStartdato(
        fakturaserieDto.perioder
    )

    private fun mapFullmektig(fullmektigDto: Fullmektig?): Fullmektig? {
        if (fullmektigDto != null) {
            return Fullmektig(
                fodselsnummer = fullmektigDto.fodselsnummer,
                organisasjonsnummer = fullmektigDto.organisasjonsnummer,
            )
        }
        return null
    }

    private fun mapStartdato(perioder: List<FakturaseriePeriode>): LocalDate {
        return perioder.minByOrNull { it.startDato }!!.startDato
    }

    private fun mapSluttdato(perioder: List<FakturaseriePeriode>): LocalDate {
        return perioder.maxByOrNull { it.sluttDato }!!.sluttDato
    }
}
