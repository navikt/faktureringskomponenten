package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Model for fakturaserie, inneholder informasjon for alle bestilte og planlagte fakturaer")
data class FakturaserieResponseDto(

    val id: Long? = null,

    @Schema(description = "Unik identifikator som saksbehandlingssystemet kjenner igjen")
    val vedtaksId: String = "",

    @Schema(description = "Informasjon om hva bruker betaler")
    val fakturaGjelder: String = "",

    @Schema(description = "Fødselsnummer for fakturamottaker, 11 siffer")
    val fodselsnummer: BigDecimal = BigDecimal(0),

    @Embedded
    val fullmektig: FullmektigDto? = null,

    @Schema(description = "Referanse for bruker/mottaker")
    val referanseBruker: String = "",

    @Schema(description = "Referanse for NAV")
    val referanseNAV: String = "",

    @Schema(description = "Startdato for fakturaserie")
    val startdato: LocalDate = LocalDate.now(),

    @Schema(description = "Sluttdato for fakturaserie")
    val sluttdato: LocalDate = LocalDate.now(),

    var status: FakturaserieStatus = FakturaserieStatus.OPPRETTET,

    val intervall: FakturaserieIntervallDto = FakturaserieIntervallDto.MANEDLIG,

    @Schema(description = "Tidspunkt for opprettelse av fakturaserien")
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @Schema(description = "Liste over planlagte fakturaer")
    val faktura: List<FakturaResponseDto> = listOf()
) {
    @Override
    override fun toString(): String {
        return "vedtaksId: $vedtaksId, " +
                "fakturaGjelder: $fakturaGjelder, " +
                "referanseNAV: $referanseNAV, " +
                "startdato: $startdato, " +
                "sluttDato: $sluttdato, " +
                "faktura: $faktura"
    }
}
