package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.controller.dto.*
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class FakturaserieMapper(@Autowired val fakturaMapper: FakturaMapper) {

    fun tilFakturaserie(fakturaserieDto: FakturaserieDto, startDato: LocalDate? = null): Fakturaserie {
        val startDatoForHelePerioden = startDato ?: mapStartdato(fakturaserieDto.perioder)
        val sluttDatoForHelePerioden = mapSluttdato(fakturaserieDto.perioder)
        return Fakturaserie(
            id = null,
            vedtaksId = fakturaserieDto.vedtaksId,
            fakturaGjelder = fakturaserieDto.fakturaGjelder,
            fodselsnummer = BigDecimal(fakturaserieDto.fodselsnummer),
            fullmektig = mapFullmektig(fakturaserieDto.fullmektig),
            referanseBruker = fakturaserieDto.referanseBruker,
            referanseNAV = fakturaserieDto.referanseNAV,
            startdato = startDatoForHelePerioden,
            sluttdato = sluttDatoForHelePerioden,
            intervall = FakturaserieIntervall.valueOf(fakturaserieDto.intervall.name),
            faktura = fakturaMapper.tilListeAvFaktura(
                fakturaserieDto.perioder,
                startDatoForHelePerioden,
                sluttDatoForHelePerioden,
                fakturaserieDto.intervall
            ),
        )
    }

    private fun mapFullmektig(fullmektigDto: FullmektigDto?): Fullmektig? {
        if (fullmektigDto != null) {
            return Fullmektig(
                fodselsnummer = if (fullmektigDto.fodselsnummer != null) BigDecimal(fullmektigDto.fodselsnummer) else null,
                organisasjonsnummer = fullmektigDto.organisasjonsnummer,
                kontaktperson = fullmektigDto.kontaktperson
            )
        }
        return null
    }

    private fun mapStartdato(perioder: List<FakturaseriePeriodeDto>): LocalDate {
        return perioder.minByOrNull { it.startDato }!!.startDato
    }

    private fun mapSluttdato(perioder: List<FakturaseriePeriodeDto>): LocalDate {
        return perioder.maxByOrNull { it.sluttDato }!!.sluttDato
    }
}
