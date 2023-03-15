package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import no.nav.faktureringskomponenten.controller.validators.ErFodselsnummer
import no.nav.faktureringskomponenten.controller.validators.ErIkkeOverlappendePerioder
import no.nav.faktureringskomponenten.controller.validators.IkkeDuplikatVedtaksId
import no.nav.faktureringskomponenten.domain.models.FakturaGjelder
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall

@Schema(description = "DTO for fullstendig informasjon om alle planlagte fakturaer")
data class FakturaserieRequestDto(

    @Schema(description = "Unik identifikator som saksbehandlingssystemet kjenner igjen")
    @IkkeDuplikatVedtaksId
    val vedtaksId: String,

    @Schema(description = "Fødselsnummer for fakturamottaker, 11 siffer")
    @ErFodselsnummer
    val fodselsnummer: String,

    val fullmektig: FullmektigDto?,

    @Schema(
        description = "Referanse for bruker/mottaker",
        example = "Vedtak om medlemskap datert 01.12.2022",
        maxLength = 240
    )
    @NotBlank(message = "Du må oppgi referanseBruker")
    val referanseBruker: String,

    @Schema(
        description = "Referanse for NAV",
        example = "Medlemskap og avgift",
        maxLength = 240
    )
    @NotBlank(message = "Du må oppgi referanseNAV")
    val referanseNAV: String,

    @Schema(
        description = "Informasjon om hvilket tema fakturaen gjelder",
        example = "TRY",
    )
    @NotNull(message = "Du må oppgi fakturaGjelder")
    val fakturaGjelder: FakturaGjelder,

    @Schema(
        description = "Betalingsintervall",
        example = "KVARTAL",
    )
    val intervall: FakturaserieIntervall = FakturaserieIntervall.MANEDLIG,

    @Schema(description = "Liste av betalingsperioder, kan ikke være overlappende")
    @NotEmpty(message = "Du må oppgi minst én periode")
    @ErIkkeOverlappendePerioder
    val perioder: List<FakturaseriePeriodeDto>
) {
    @Override
    override fun toString(): String {
        return "vedtaksId: $vedtaksId, intervall: $intervall, perioder: $perioder"
    }
}