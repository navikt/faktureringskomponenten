package no.nav.faktureringskomponenten.controller.dto

import no.nav.faktureringskomponenten.validators.ErFodselsnummer
import no.nav.faktureringskomponenten.validators.ErIkkeOverlappendePerioder
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

data class FakturaserieDto(
    val vedtaksId: String,
    @field:ErFodselsnummer
    val fodselsnummer: String,
    val fullmektig: FullmektigDto?,
    val referanseBruker: String?,
    @field:NotBlank
    val referanseNAV: String,
    @field:NotBlank
    val fakturaGjelder: String,
    val intervall: FakturaserieIntervallDto = FakturaserieIntervallDto.MANEDLIG,
    @field:NotEmpty(message="Du må oppgi minst én periode.")
    @field:ErIkkeOverlappendePerioder
    val perioder: List<FakturaseriePeriodeDto>
)