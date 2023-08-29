package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no.nav.faktureringskomponenten.controller.validators.ErFodselsnummer
import no.nav.faktureringskomponenten.controller.validators.IkkeDuplikatVedtaksId
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall

@Schema(description = "DTO for fullstendig informasjon om alle planlagte fakturaer")
data class FakturaserieRequestDto(

    @field:Schema(description = "Unik identifikator som saksbehandlingssystemet kjenner igjen")
    @field:IkkeDuplikatVedtaksId
    val vedtaksId: String,

    val saksummer: String?,

    @field:Schema(description = "Fødselsnummer for fakturamottaker, 11 siffer")
    @field:ErFodselsnummer
    val fodselsnummer: String,

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
        description = "Betalingsintervall",
        example = "KVARTAL",
    )
    val intervall: FakturaserieIntervall = FakturaserieIntervall.KVARTAL,

    @field:Schema(description = "Liste av betalingsperioder, kan ikke være tom")
    @field:NotEmpty(message = "Du må oppgi minst én periode")
    val perioder: List<FakturaseriePeriodeDto>
) {
    @Override
    override fun toString(): String {
        return "vedtaksId: $vedtaksId, intervall: $intervall, perioder: $perioder"
    }
}