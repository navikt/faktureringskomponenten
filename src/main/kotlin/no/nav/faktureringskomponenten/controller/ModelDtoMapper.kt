package no.nav.faktureringskomponenten.controller

import no.nav.faktureringskomponenten.controller.dto.*
import no.nav.faktureringskomponenten.domain.models.Fakturaserie

object ModelDtoMapper {

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
}
