package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.models.FakturaGjelder
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Model for fakturaserie, inneholder informasjon for alle bestilte og planlagte fakturaer")
data class FakturaserieResponseDto(

    @Schema(description = "Unik identifikator som saksbehandlingssystemet kjenner igjen")
    val vedtaksId: String,

    @Schema(description = "Informasjon om hva bruker betaler")
    val fakturaGjelder: FakturaGjelder,

    @Schema(description = "Fødselsnummer for fakturamottaker, 11 siffer")
    val fodselsnummer: String,

    val fullmektig: FullmektigDto?,

    @Schema(description = "Referanse for bruker/mottaker")
    val referanseBruker: String,

    @Schema(description = "Referanse for NAV")
    val referanseNAV: String,

    @Schema(description = "Startdato for fakturaserie")
    val startdato: LocalDate,

    @Schema(description = "Sluttdato for fakturaserie")
    val sluttdato: LocalDate,

    val status: FakturaserieStatus,

    val intervall: FakturaserieIntervall,

    @Schema(description = "Tidspunkt for opprettelse av fakturaserien")
    val opprettetTidspunkt: LocalDateTime,

    @Schema(description = "Liste over planlagte fakturaer")
    val faktura: List<FakturaResponseDto>
)
