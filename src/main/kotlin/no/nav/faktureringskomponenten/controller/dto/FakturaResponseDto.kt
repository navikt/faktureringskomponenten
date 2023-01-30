package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import java.time.LocalDate

@Schema(description = "Model for en faktura i fakturaserien")
data class FakturaResponseDto(

    val id: Long?,

    @Schema(
        description = "Dato for når faktura bestilles til OEBS"
    )
    val datoBestilt: LocalDate,


    var status: FakturaStatus,

    @Schema(
        description = "Fakturalinjer i fakturaen"
    )
    val fakturaLinje: List<FakturaLinjeResponseDto>,

    val fakturaserieId: Long?

) {

    @Schema(
        description = "Startdato for perioden"
    )
    fun getPeriodeFra(): LocalDate {
        return fakturaLinje.minOf { it.periodeFra }
    }

    @Schema(
        description = "Sluttdato for perioden"
    )
    fun getPeriodeTil(): LocalDate {
        return fakturaLinje.maxOf { it.periodeTil }
    }

    @Override
    override fun toString(): String {
        return "$id: datoBestilt: $datoBestilt, status: $status, fakturaLinje: $fakturaLinje"
    }
}
