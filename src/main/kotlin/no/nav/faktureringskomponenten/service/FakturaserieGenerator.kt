package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class FakturaserieGenerator(
    val fakturaGenerator: FakturaGenerator
) {

    fun lagFakturaserie(fakturaserieDto: FakturaserieDto, startDato: LocalDate? = null, avregningsfaktura: Faktura? = null): Fakturaserie {
        val startDatoForSamletPeriode =
            if (avregningsfaktura != null) avregningsfaktura.getPeriodeTil().plusDays(1) else startDato ?: mapStartdato(
                fakturaserieDto.perioder
            )
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
            faktura = fakturaerForSamletPeriode + listOfNotNull(avregningsfaktura)
        )
    }

    fun lagKrediteringFakturaSerie(fakturaserie: Fakturaserie): Fakturaserie {
        return Fakturaserie(
            id = null,
            referanse = fakturaserie.referanse,
            fakturaGjelderInnbetalingstype = fakturaserie.fakturaGjelderInnbetalingstype,
            fodselsnummer = fakturaserie.fodselsnummer,
            fullmektig = mapFullmektig(fakturaserie.fullmektig),
            referanseBruker = fakturaserie.referanseBruker,
            referanseNAV = fakturaserie.referanseNAV,
            startdato = fakturaserie.startdato,
            sluttdato = fakturaserie.sluttdato,
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
