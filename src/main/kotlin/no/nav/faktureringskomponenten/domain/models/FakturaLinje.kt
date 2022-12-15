package no.nav.faktureringskomponenten.domain.models

import java.math.BigDecimal
import java.time.LocalDate
import javax.persistence.*

@Entity
@Table(name = "faktura_linje")
data class FakturaLinje(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @Column(name = "periode_fra", nullable = false)
    val periodeFra: LocalDate,

    @Column(name = "periode_til", nullable = false)
    val periodeTil: LocalDate,

    @Column(name = "beskrivelse", nullable = false)
    val beskrivelse: String,

    @Column(name = "belop", nullable = false)
    val belop: BigDecimal,

    @Column(name = "enhetspris_per_maned", nullable= false)
    val enhetsprisPerManed: BigDecimal
)
