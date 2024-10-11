package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode

@Schema(description = "Model for å regne ut totalbeløp")
data class BeregnTotalBeløpDto(
    @Schema(description = "FakturaseriePerioder")
    @field:NotEmpty(message = "FakturaseriePerioder kan ikke være tom")
    val fakturaseriePerioder: List<FakturaseriePeriode>
)
