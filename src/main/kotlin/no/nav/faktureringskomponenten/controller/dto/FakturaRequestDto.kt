package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import no.nav.faktureringskomponenten.controller.validators.ErFodselsnummer
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "DTO for fullstendig informasjon om en enkelt faktura")
data class FakturaRequestDto(

    @field:Schema(description = "Fødselsnummer for fakturamottaker, 11 siffer")
    @field:ErFodselsnummer
    val fodselsnummer: String,

    @field:Schema(description = "Referanse til tidligere fakturaserie ifm kreditering")
    var fakturaserieReferanse: String?,

    val fullmektig: FullmektigDto?,

    @field:Schema(
        description = "Referanse for bruker/mottaker",
        example = "Vedtak om medlemskap datert 01.12.2022",
        maxLength = 240
    )
    @field:NotBlank(message = "Du må oppgi referanseBruker")
    val referanseBruker: String,

    @field:Schema(
        description = "Referanse for NAV",
        example = "Medlemskap og avgift",
        maxLength = 240
    )
    @field:NotBlank(message = "Du må oppgi referanseNAV")
    val referanseNAV: String,

    @field:Schema(
        description = "Informasjon om hvilken innbetalingstype fakturaen gjelder",
        example = "TRYGDEAVGIFT",
    )
    @field:NotNull(message = "Du må oppgi fakturaGjelderInnbetalingstype")
    val fakturaGjelderInnbetalingstype: Innbetalingstype,

    @Schema(
        description = "Enhetspris mottaker betaler per måned",
        example = "1000",
    )
    val belop: BigDecimal,


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
) {
    @Override
    override fun toString(): String {
        return "referanseNav: $referanseNAV referanseBruker: $referanseBruker"
    }
}
