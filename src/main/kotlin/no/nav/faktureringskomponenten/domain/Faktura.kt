package no.nav.faktureringskomponenten.domain

import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.ManyToOne

@Entity
data class Faktura(

    @javax.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,
    @ManyToOne
    val fakturaserie: Fakturaserie,

    val total_belop: BigDecimal,
    periode_fra DATE
    periode_til DATE
    dato_sendt DATE
    status VARCHAR
    dato_betalt DATE
    beskrivelse VARCHAR
    )