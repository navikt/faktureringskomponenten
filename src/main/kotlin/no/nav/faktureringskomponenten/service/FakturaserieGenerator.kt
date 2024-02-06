package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.*
import org.springframework.stereotype.Component
import ulid.ULID
import java.time.LocalDate

@Component
class FakturaserieGenerator(
    val fakturaGenerator: FakturaGenerator
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

    private fun finnStartDatoForSamletPeriode(
        avregningsfakturaSistePeriodeTil: LocalDate?,
        startDato: LocalDate?,
        fakturaserieDto: FakturaserieDto
    ) = avregningsfakturaSistePeriodeTil?.plusDays(1) ?: startDato ?: mapStartdato(
        fakturaserieDto.perioder
    )

    fun lagKrediteringFakturaSerie(fakturaserie: Fakturaserie): Fakturaserie {
        return Fakturaserie(
            id = null,
            referanse = ULID.randomULID(),
            fakturaGjelderInnbetalingstype = fakturaserie.fakturaGjelderInnbetalingstype,
            fodselsnummer = fakturaserie.fodselsnummer,
            fullmektig = mapFullmektig(fakturaserie.fullmektig),
            referanseBruker = fakturaserie.referanseBruker,
            referanseNAV = fakturaserie.referanseNAV,
            startdato = fakturaserie.startdato,
            sluttdato = fakturaserie.sluttdato,
            status = FakturaserieStatus.OPPRETTET,
            intervall = fakturaserie.intervall,
            faktura = fakturaGenerator.lagKreditnota(fakturaserie.bestilteFakturaer())
        )
    }

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
