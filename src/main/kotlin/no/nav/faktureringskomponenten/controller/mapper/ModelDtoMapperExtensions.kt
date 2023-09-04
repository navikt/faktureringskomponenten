package no.nav.faktureringskomponenten.controller.mapper

import no.nav.faktureringskomponenten.controller.dto.*
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.service.FakturaserieDto
import java.util.*

val Fakturaserie.tilFakturaserieResponseDto: FakturaserieResponseDto
    get() = FakturaserieResponseDto(
        referanseId = this.referanseId,
        fakturaGjelderInnbetalingstype = this.fakturaGjelderInnbetalingstype,
        fodselsnummer = this.fodselsnummer,
        fullmektig = this.fullmektig?.tilDto,
        referanseBruker = this.referanseBruker,
        referanseNAV = this.referanseNAV,
        startdato = this.startdato,
        sluttdato = this.sluttdato,
        status = this.status,
        intervall = this.intervall,
        opprettetTidspunkt = this.opprettetTidspunkt,
        faktura = this.faktura.map { it.tilResponseDto },
        erstattetMed = this.erstattetMed
    )

val FakturaMottatt.tilFakturaTilbakemeldingResponseDto: FakturaTilbakemeldingResponseDto
    get() = FakturaTilbakemeldingResponseDto(
        fakturaBelop = this.fakturaBelop,
        ubetaltBelop = this.ubetaltBelop,
        dato = this.dato,
        status = this.status,
        feilmelding = this.feilMelding
    )

val FakturaserieRequestDto.tilFakturaserieDto: FakturaserieDto
    get() = FakturaserieDto(
        referanseId = UUID.randomUUID().toString(),
        fodselsnummer = this.fodselsnummer,
        fullmektig = this.fullmektig?.tilFullmektig,
        referanseBruker = this.referanseBruker,
        referanseNAV = this.referanseNAV,
        fakturaGjelderInnbetalingstype = this.fakturaGjelderInnbetalingstype,
        intervall = this.intervall,
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

private val Fullmektig.tilDto: FullmektigDto
    get() = FullmektigDto(
        fodselsnummer = this.fodselsnummer,
        organisasjonsnummer = this.organisasjonsnummer,
        kontaktperson = this.kontaktperson
    )

private val FullmektigDto.tilFullmektig: Fullmektig
    get() = Fullmektig(
        fodselsnummer = this.fodselsnummer,
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

