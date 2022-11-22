package no.nav.faktureringskomponenten.controller.dto

import java.math.BigDecimal
import java.time.LocalDate

data class FakturaseriePeriodeDto(

    val enhetsprisPerManed: BigDecimal,

    val startDato: LocalDate,

    val sluttDato: LocalDate,

    val beskrivelse: String
)
