package no.nav.faktureringskomponenten.domain.models

import java.math.BigDecimal
import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
data class Fullmektig(

    @Column(name = "fullmektig_fodselsnummer", nullable = true)
    val fodselsnummer: BigDecimal?,

    @Column(name = "fullmektig_organisasjonsnummer", nullable = true)
    val organisasjonsnummer: String?,

    @Column(name = "fullmektig_kontaktperson", nullable = true)
    val kontaktperson: String?
)