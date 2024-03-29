package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Model for en faktura i fakturaserien")
data class FakturaResponseDto(

    @Schema(description = "Unik identifikator av faktura")
    val fakturaReferanse: String,

    @Schema(description = "Dato for når faktura bestilles til OEBS")
    val datoBestilt: LocalDate,

    @Schema(description = "Dato for når faktura sist ble oppdatert")
    val sistOppdatert: LocalDateTime,

    var status: FakturaStatus,

    @Schema(description = "Fakturalinjer i fakturaen")
    val fakturaLinje: List<FakturaLinjeResponseDto>,

    @Schema(description = "Startdato for perioden")
    val periodeFra: LocalDate,

    @Schema(description = "Sluttdato for perioden")
    val periodeTil: LocalDate,

    val eksternFakturaStatus: List<FakturaTilbakemeldingResponseDto>,

    val eksternFakturaNummer: String? = "",
)
