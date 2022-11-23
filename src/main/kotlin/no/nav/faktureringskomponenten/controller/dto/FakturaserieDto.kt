package no.nav.faktureringskomponenten.controller.dto

import no.nav.faktureringskomponenten.validators.ErFodselsnummer
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotEmpty

data class FakturaserieDto(
    val vedtaksnummer: String,
    @field:ErFodselsnummer
    val fodselsnummer: String,
    val fullmektig: FullmektigDto?,
    val referanseBruker: String?,
    @field:NotBlank
    val referanseNAV: String,
    val intervall: FaktureringsIntervall = FaktureringsIntervall.MANEDLIG,
    @field:NotEmpty(message="Du må oppgi minst én periode.")
    val perioder: List<FakturaseriePeriodeDto>
)