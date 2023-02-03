package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Schema(
    description = "Fullmektig som mottar faktura"
)
@Embeddable
data class Fullmektig(

    @Schema(
        description = "Fødselsnummer for fullmektig, 11 siffer",
    )
    @Column(name = "fullmektig_fodselsnummer", nullable = true)
    val fodselsnummer: String? = null,


    @Schema(
        description = "Organisasjonsnummer for fullmektig"
    )
    @Column(name = "fullmektig_organisasjonsnummer", nullable = true)
    val organisasjonsnummer: String? = null,


    @Schema(
        description = "Navn på kontaktperson hos fullmektig",
    )
    @Column(name = "fullmektig_kontaktperson", nullable = true)
    val kontaktperson: String? = null
)
