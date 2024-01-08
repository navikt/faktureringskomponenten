package no.nav.faktureringskomponenten.controller.mapper

import no.nav.faktureringskomponenten.controller.dto.*
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.service.FakturamottakerDto
import no.nav.faktureringskomponenten.service.FakturaserieDto
import ulid.ULID
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

val Fakturaserie.tilFakturaserieResponseDto: FakturaserieResponseDto
    get() = FakturaserieResponseDto(
        fakturaserieReferanse = this.referanse,
        fakturaGjelderInnbetalingstype = this.fakturaGjelderInnbetalingstype,
        fodselsnummer = this.fodselsnummer,
        fullmektig = this.fullmektig?.tilDto,
        referanseBruker = this.referanseBruker,
        referanseNAV = this.referanseNAV,
        startdato = this.startdato,
        sluttdato = this.sluttdato,
        status = this.status,
        intervall = this.intervall,
        opprettetTidspunkt = LocalDateTime.ofInstant(this.opprettetTidspunkt, ZoneId.systemDefault()),
        faktura = this.faktura.map { it.tilResponseDto },
    )

val FakturaserieRequestDto.tilFakturaserieDto: FakturaserieDto
    get() = FakturaserieDto(
        fakturaserieReferanse = ULID.randomULID(),
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
    )

private val FullmektigDto.tilFullmektig: Fullmektig
    get() = Fullmektig(
        fodselsnummer = this.fodselsnummer,
        organisasjonsnummer = this.organisasjonsnummer,
    )

private val Faktura.tilResponseDto: FakturaResponseDto
    get() = FakturaResponseDto(
        datoBestilt = this.datoBestilt,
        sistOppdatert = this.sistOppdatert,
        status = this.status,
        fakturaLinje = this.fakturaLinje.map { it.tilResponseDto },
        periodeFra = this.getPeriodeFra(),
        periodeTil = this.getPeriodeTil(),
        eksternFakturaStatus = this.eksternFakturaStatus.map { it.tilResponseDto },
        eksternFakturaNummer = this.eksternFakturaNummer
    )

private val EksternFakturaStatus.tilResponseDto: FakturaTilbakemeldingResponseDto
    get() = FakturaTilbakemeldingResponseDto(
        dato = this.dato,
        status = this.status,
        fakturaBelop = this.fakturaBelop,
        ubetaltBelop = this.ubetaltBelop,
        feilmelding = this.feilMelding,
    )


private val FakturaLinje.tilResponseDto: FakturaLinjeResponseDto
    get() = FakturaLinjeResponseDto(
        periodeFra = this.periodeFra,
        periodeTil = this.periodeTil,
        beskrivelse = this.beskrivelse,
        belop = this.belop,
        antall = this.antall,
        enhetsprisPerManed = this.enhetsprisPerManed
    )

val FakturamottakerRequestDto.tilFakturamottakerDto: FakturamottakerDto
    get() = FakturamottakerDto(
        fullmektig = this.fullmektig?.tilFullmektig,
    )