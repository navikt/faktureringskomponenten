package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import javax.persistence.Column
import javax.persistence.Embeddable

@Schema(
    description = "Fullmektig som mottar faktura"
)
@Embeddable
data class Fullmektig(

    @Schema(
        description = "Fødselsnummer for fullmektig, 11 siffer",
    )
    @Column(name = "fullmektig_fodselsnummer", nullable = true)
    val fodselsnummer: BigDecimal?,


    @Schema(
        description = "Organisasjonsnummer for fullmektig"
    )
    @Column(name = "fullmektig_organisasjonsnummer", nullable = true)
    val organisasjonsnummer: String?,


    @Schema(
        description = "Navn på kontaktperson hos fullmektig",
    )
    @Column(name = "fullmektig_kontaktperson", nullable = true)
    val kontaktperson: String?
)