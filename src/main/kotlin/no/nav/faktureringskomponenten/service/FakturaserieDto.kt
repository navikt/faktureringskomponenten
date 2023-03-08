package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.controller.dto.FullmektigDto
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.domain.models.Fullmektig

data class FakturaserieDto(
    val vedtaksId: String,

    val fodselsnummer: String,

    val fullmektig: Fullmektig?,

    val referanseBruker: String,

    val referanseNAV: String,

    val fakturaGjelder: String,

    val intervall: FakturaserieIntervall = FakturaserieIntervall.MANEDLIG,

    val perioder: List<FakturaseriePeriode>) {
}