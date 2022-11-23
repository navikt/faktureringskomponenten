package no.nav.faktureringskomponenten.domain.models

import javax.persistence.Column
import javax.persistence.Embeddable

@Embeddable
data class Fullmektig (
    
    @Column(name = "fullmektig_fodselsnummer", nullable = true)
    val fodselsnummer: String,

    @Column(name = "fullmektig_organisasjonsnummer", nullable = true)
    val organisasjonsnummer: String,

    @Column(name = "fullmektig_kontaktperson", nullable = true)
    val kontaktperson: String
)