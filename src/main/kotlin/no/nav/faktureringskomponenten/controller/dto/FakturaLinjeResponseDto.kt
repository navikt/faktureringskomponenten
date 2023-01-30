package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Schema(
    description = "Model for en linje i fakturaen. Liste av linjer gir grunnlag for hele fakturabeløpet"
)
data class FakturaLinjeResponseDto(

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,


    @Schema(
        description = "Startdato for perioden"
    )
    val periodeFra: LocalDate,


    @Schema(
        description = "Sluttdato for perioden"
    )
    val periodeTil: LocalDate,


    @Schema(
        description = "Beskrivelse på hva grunnlaget for faktureringsbeløpet for perioden. Perioden blir automatisk lagt på",
    )
    val beskrivelse: String,


    @Schema(
        description = "Totalbeløp for hele fakturalinjen"
    )
    val belop: BigDecimal,


    @Schema(
        description = "Enhetspris per måned"
    )
    val enhetsprisPerManed: BigDecimal
) {

    @Override
    override fun toString(): String {
        return "beskrivelse: $beskrivelse, belop: $belop"
    }
}
