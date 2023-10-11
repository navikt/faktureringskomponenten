package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.faktureringskomponenten.domain.models.EksternFakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import java.math.BigDecimal
import java.time.LocalDate

@Schema(description = "Model for en faktura i fakturaserien")
data class FakturaResponseDto(

    val id: Long?,

    @Schema(description = "Dato for når faktura bestilles til OEBS")
    val datoBestilt: LocalDate,

    @Schema(description = "Dato for når faktura sist ble oppdatert")
    val sistOppdatert: LocalDate,

    var status: FakturaStatus,

    @Schema(description = "Fakturalinjer i fakturaen")
    val fakturaLinje: List<FakturaLinjeResponseDto>,

    @Schema(description = "Startdato for perioden")
    val periodeFra: LocalDate,

    @Schema(description = "Sluttdato for perioden")
    val periodeTil: LocalDate,

    val eksternFakturaStatus: List<FakturaTilbakemeldingResponseDto>,

    val nyesteFakturaStatus: FakturaTilbakemeldingResponseDto? = null
)
