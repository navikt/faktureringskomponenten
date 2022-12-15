package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.faktureringskomponenten.validators.ErFodselsnummer
import no.nav.faktureringskomponenten.validators.ErIkkeOverlappendePerioder
import no.nav.faktureringskomponenten.validators.IkkeDuplikatVedtaksId
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

@Schema(description = "DTO for fullstendig informasjon om alle planlagte fakturaer")
data class FakturaserieDto(

    @field:Schema(
        description = "Unik identifikator som saksbehandlingssystemet kjenner igjen",
    )
    @field:IkkeDuplikatVedtaksId
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
)