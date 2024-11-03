package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import java.math.BigDecimal
import java.time.LocalDate

data class EnkeltFakturaDto(
    val referanse: String,

    val tidligereFakturaserieReferanse: String?,

    val fodselsnummer: String,

    val fullmektig: Fullmektig?,

    val referanseBruker: String,

    val referanseNAV: String,

    val fakturaGjelderInnbetalingstype: Innbetalingstype,

    val intervall: FakturaserieIntervall = FakturaserieIntervall.SINGEL,

    val belop: BigDecimal,

    val startDato: LocalDate,

    val sluttDato: LocalDate,

    val beskrivelse: String
)