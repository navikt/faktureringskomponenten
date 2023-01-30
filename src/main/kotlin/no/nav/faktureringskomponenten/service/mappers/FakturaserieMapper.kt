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

val Fakturaserie.tilResponseDto: FakturaserieResponseDto
    get() {
        return FakturaserieResponseDto(
            id = this.id,
            vedtaksId = this.vedtaksId,
            fakturaGjelder = this.fakturaGjelder,
            fodselsnummer = this.fodselsnummer,
            fullmektig = this.fullmektig.let {
                FullmektigDto(
                    fodselsnummer = it?.fodselsnummer.toString(),
                    organisasjonsnummer = it?.organisasjonsnummer,
                    kontaktperson = it?.kontaktperson
                )
            },
            referanseBruker = this.referanseBruker,
            referanseNAV = this.referanseNAV,
            startdato = this.startdato,
            sluttdato = this.sluttdato,
            status = this.status,
            intervall = FakturaserieIntervallDto.valueOf(this.intervall.name),
            opprettetTidspunkt = this.opprettetTidspunkt,
            faktura = this.faktura.map { FakturaResponseDto(
                id = it.id,
                datoBestilt = it.datoBestilt,
                status = it.status,
                fakturaLinje = it.fakturaLinje.map { fi -> FakturaLinjeResponseDto(
                    id = fi.id,
                    periodeFra = fi.periodeFra,
                    periodeTil = fi.periodeTil,
                    beskrivelse = fi.beskrivelse,
                    belop = fi.belop,
                    enhetsprisPerManed = fi.enhetsprisPerManed
                ) },
                fakturaserieId = it.getFakturaserieId(),
                periodeFra = it.getPeriodeFra(),
                periodeTil = it.getPeriodeTil()
            ) }
        )
    }

