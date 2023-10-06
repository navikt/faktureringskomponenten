package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "Model for en linje i fakturaen. Liste av linjer gir grunnlag for hele fakturabeløpet")
data class FakturaLinjeResponseDto(

    @Schema(description = "Startdato for perioden")
    val periodeFra: LocalDate,

    @Schema(description = "Sluttdato for perioden")
    val periodeTil: LocalDate,

    @Schema(description = "Beskrivelse på hva grunnlaget for faktureringsbeløpet for perioden. Perioden blir automatisk lagt på")
    val beskrivelse: String,

    @Schema(description = "Totalbeløp for hele fakturalinjen")
    val belop: BigDecimal,

    @Schema(description = "Antall enheter")
    val antall: BigDecimal,

    @Schema(description = "Enhetspris")
    val enhetsprisPerManed: BigDecimal
)
