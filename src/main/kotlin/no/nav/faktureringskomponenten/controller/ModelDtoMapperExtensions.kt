package no.nav.faktureringskomponenten.controller

import no.nav.faktureringskomponenten.controller.dto.*
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.Fullmektig

val Fakturaserie.tilResponseDto: FakturaserieResponseDto
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

private val Fullmektig.tilDto
    get() = FullmektigDto(
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

