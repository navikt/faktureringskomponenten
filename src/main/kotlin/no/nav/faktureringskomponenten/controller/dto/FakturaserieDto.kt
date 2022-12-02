package no.nav.faktureringskomponenten.controller.dto

import no.nav.faktureringskomponenten.validators.ErFodselsnummer
import no.nav.faktureringskomponenten.validators.ErIkkeOverlappendePerioder
import no.nav.faktureringskomponenten.validators.IkkeDuplikatVedtaksId
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

data class FakturaserieDto(

    @field:IkkeDuplikatVedtaksId
    val vedtaksId: String,
    @field:ErFodselsnummer
    val fodselsnummer: String,
    val fullmektig: FullmektigDto?,
    val referanseBruker: String?,

    @field:NotBlank(message = "Du må oppgi referanseNAV")
    val referanseNAV: String,

    @field:NotBlank(message = "Du må oppgi fakturaGjelder")
    val fakturaGjelder: String,
    val intervall: FakturaserieIntervallDto = FakturaserieIntervallDto.MANEDLIG,
    @field:NotEmpty(message="Du må oppgi minst én periode.")
    @field:ErIkkeOverlappendePerioder
    val perioder: List<FakturaseriePeriodeDto>
)