package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

data class FakturaseriePeriode(

    @Schema(
        description = "Enhetspris mottaker betaler per måned",
        example = "1000",
    )
    val enhetsprisPerManed: BigDecimal,


    @Schema(
        description = "Startdato for perioden",
        example = "01.01.2022",
    )
    val startDato: LocalDate,


    @Schema(
        description = "Sluttdato for perioden",
        example = "01.05.2022",
    )
    val sluttDato: LocalDate,


    @Schema(
        description = "Beskrivelse på hva grunnlaget for faktureringsbeløpet for perioden",
        example = "Inntekt: 50.000, Dekning: Pensjonsdel, Sats: 21.8 %",
    )
    val beskrivelse: String
)
