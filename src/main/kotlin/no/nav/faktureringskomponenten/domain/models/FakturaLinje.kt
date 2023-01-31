package no.nav.faktureringskomponenten.domain.models

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "faktura_linje")
class FakturaLinje(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "periode_fra", nullable = false)
    val periodeFra: LocalDate,

    @Column(name = "periode_til", nullable = false)
    val periodeTil: LocalDate,

    @Column(name = "beskrivelse", nullable = false)
    val beskrivelse: String,

    @Column(name = "belop", nullable = false)
    val belop: BigDecimal,

    @Column(name = "enhetspris_per_maned", nullable = false)
    val enhetsprisPerManed: BigDecimal
) {
    @Override
    override fun toString(): String {
        return "beskrivelse: $beskrivelse, belop: $belop"
    }
}
