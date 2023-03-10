package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no.nav.faktureringskomponenten.controller.validators.ErFodselsnummer
import no.nav.faktureringskomponenten.controller.validators.ErIkkeOverlappendePerioder
import no.nav.faktureringskomponenten.controller.validators.IkkeDuplikatVedtaksId

@Schema(description = "DTO for fullstendig informasjon om alle planlagte fakturaer")
data class FakturaserieRequestDto(

    @field:Schema(
        description = "Unik identifikator som saksbehandlingssystemet kjenner igjen",
    )
    @IkkeDuplikatVedtaksId
    val vedtaksId: String,

    @field:Schema(
        description = "Fødselsnummer for fakturamottaker, 11 siffer",
    )
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
        example = "NAV medlemskap og avgift",
        maxLength = 240
    )
    @field:NotBlank(message = "Du må oppgi referanseNAV")
    val referanseNAV: String,

    @field:Schema(
        description = "Informasjon om hva bruker betaler",
        example = "Trygdeavgift",
    )
    @field:NotBlank(message = "Du må oppgi fakturaGjelder")
    val fakturaGjelder: String,

    @field:Schema(
        description = "Tema til vedtaket",
        example = "TRY"
    )
    @field:NotNull(message = "Du må oppgi tema")
    val tema: FakturaserieTemaDto,

    @field:Schema(
        description = "Betalingsintervall",
        example = "KVARTAL",
    )
    val intervall: FakturaserieIntervallDto = FakturaserieIntervallDto.MANEDLIG,

    @field:Schema(
        description = "Liste av betalingsperioder, kan ikke være overlappende",
    )
    @field:NotEmpty(message = "Du må oppgi minst én periode")
    @field:ErIkkeOverlappendePerioder
    val perioder: List<FakturaseriePeriodeDto>
) {
    @Override
    override fun toString(): String {
        return "vedtaksId: $vedtaksId, intervall: $intervall, perioder: $perioder"
    }
}