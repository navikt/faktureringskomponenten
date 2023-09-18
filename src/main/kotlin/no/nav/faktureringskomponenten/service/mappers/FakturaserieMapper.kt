package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import no.nav.faktureringskomponenten.service.FakturaserieDto
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class FakturaserieMapper(val fakturaMapper: FakturaMapper = FakturaMapper()) {

    fun tilFakturaserie(fakturaserieDto: FakturaserieDto, startDato: LocalDate? = null): Fakturaserie {
        val startDatoForHelePerioden = startDato ?: mapStartdato(fakturaserieDto.perioder)
        val sluttDatoForHelePerioden = mapSluttdato(fakturaserieDto.perioder)
        return Fakturaserie(
            id = null,
            referanse = fakturaserieDto.fakturaserieReferanse,
            fakturaGjelderInnbetalingstype = fakturaserieDto.fakturaGjelderInnbetalingstype,
            fodselsnummer = fakturaserieDto.fodselsnummer,
            fullmektig = mapFullmektig(fakturaserieDto.fullmektig),
            referanseBruker = fakturaserieDto.referanseBruker,
            referanseNAV = fakturaserieDto.referanseNAV,
            startdato = startDatoForHelePerioden,
            sluttdato = sluttDatoForHelePerioden,
            intervall = fakturaserieDto.intervall,
            faktura = fakturaMapper.tilListeAvFaktura(
                fakturaserieDto.perioder,
                startDatoForHelePerioden,
                sluttDatoForHelePerioden,
                fakturaserieDto.intervall
            ),
        )
    }

    private fun mapFullmektig(fullmektigDto: Fullmektig?): Fullmektig? {
        if (fullmektigDto != null) {
            return Fullmektig(
                fodselsnummer = fullmektigDto.fodselsnummer,
                organisasjonsnummer = fullmektigDto.organisasjonsnummer,
                kontaktperson = fullmektigDto.kontaktperson
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
