package no.nav.faktureringskomponenten.controller.dto

import no.nav.faktureringskomponenten.validators.ErFodselsnummer
import javax.validation.constraints.NotEmpty

data class FakturaserieDto(
    val vedtaksnummer: String,
   // @field:Size(min=11, max=11, message = "Fødselsnummer må være 11 siffer.")
    @field:ErFodselsnummer
    val fodselsnummer: String?,
    val fullmektig: FakturaserieFullmektigDto?,
    val referanseBruker: String?,
    val referanseNAV: String,
    val intervall: FaktureringsIntervall = FaktureringsIntervall.MANEDLIG,
    @field:NotEmpty(message="Du må oppgi minst én periode,.")
    val perioder: List<FakturaseriePeriodeDto>?
)