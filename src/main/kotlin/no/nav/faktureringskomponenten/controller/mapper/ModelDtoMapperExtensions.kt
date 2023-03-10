package no.nav.faktureringskomponenten.controller.mapper

import no.nav.faktureringskomponenten.controller.dto.*
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.service.FakturaserieDto

val Fakturaserie.tilFakturaserieResponseDto: FakturaserieResponseDto
    get() = FakturaserieResponseDto(
        vedtaksId = this.vedtaksId,
        fakturaGjelder = this.fakturaGjelder,
        fodselsnummer = this.fodselsnummer,
        fullmektig = this.fullmektig?.tilDto,
        referanseBruker = this.referanseBruker,
        referanseNAV = this.referanseNAV,
        startdato = this.startdato,
        sluttdato = this.sluttdato,
        status = this.status,
        intervall = FakturaserieIntervallDto.valueOf(this.intervall.name),
        opprettetTidspunkt = this.opprettetTidspunkt,
        faktura = this.faktura.map { it.tilResponseDto }
    )


val FakturaserieRequestDto.tilFakturaserieDto: FakturaserieDto
    get() = FakturaserieDto(
        vedtaksId = this.vedtaksId,
        fodselsnummer = this.fodselsnummer,
        fullmektig = this.fullmektig?.tilFullmektig,
        referanseBruker = this.referanseBruker,
        referanseNAV = this.referanseNAV,
        fakturaGjelder = this.fakturaGjelder,
        intervall = this.intervall.tilFakturaserieIntervall(),
        perioder = this.perioder.tilFakturaseriePeriodeList
    )

val List<FakturaseriePeriodeDto>.tilFakturaseriePeriodeList: List<FakturaseriePeriode>
    get() = map {
        FakturaseriePeriode(
            enhetsprisPerManed = it.enhetsprisPerManed,
            startDato = it.startDato,
            sluttDato = it.sluttDato,
            beskrivelse = it.beskrivelse
        )
    }

fun FakturaserieIntervallDto.tilFakturaserieIntervall(): FakturaserieIntervall {
    return FakturaserieIntervall.valueOf(this.name.uppercase())
}

fun FakturaserieIntervall.tilFakturaserieIntervallDto(): FakturaserieIntervallDto {
    return FakturaserieIntervallDto.valueOf(this.name.uppercase())
}

private val Fullmektig.tilDto: FullmektigDto
    get() = FullmektigDto(
        fodselsnummer = this.fodselsnummer.toString(),
        organisasjonsnummer = this.organisasjonsnummer,
        kontaktperson = this.kontaktperson
    )

private val FullmektigDto.tilFullmektig: Fullmektig
    get() = Fullmektig(
        fodselsnummer = this.fodselsnummer.toString(),
        organisasjonsnummer = this.organisasjonsnummer,
        kontaktperson = this.kontaktperson
    )

private val Faktura.tilResponseDto: FakturaResponseDto
    get() = FakturaResponseDto(
        id = this.id,
        datoBestilt = this.datoBestilt,
        status = this.status,
        fakturaLinje = this.fakturaLinje.map { it.tilResponseDto },
        periodeFra = this.getPeriodeFra(),
        periodeTil = this.getPeriodeTil()
    )

private val FakturaLinje.tilResponseDto: FakturaLinjeResponseDto
    get() = FakturaLinjeResponseDto(
        periodeFra = this.periodeFra,
        periodeTil = this.periodeTil,
        beskrivelse = this.beskrivelse,
        belop = this.belop,
        enhetsprisPerManed = this.enhetsprisPerManed
    )

