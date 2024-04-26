package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import java.time.LocalDate

@Schema(description = "Model for å regne ut totalbeløp")
data class BeregnTotalBeløpDto(

    @Schema(description = "Startdato for perioden")
    val periodeFra: LocalDate,

    @Schema(description = "Sluttdato for perioden")
    val periodeTil: LocalDate,

    @Schema(description = "FakturaseriePerioder")
    val fakturaseriePerioder: List<FakturaseriePeriode>
)