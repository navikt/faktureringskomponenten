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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avregning_faktura_id")
    val referertFakturaVedAvregning: Faktura? = null,

    @Column(name = "periode_fra", nullable = false)
    val periodeFra: LocalDate = LocalDate.now(),

    @Column(name = "periode_til", nullable = false)
    val periodeTil: LocalDate = LocalDate.now(),

    @Column(name = "beskrivelse", nullable = false)
    val beskrivelse: String = "",

    @Column(name = "antall", nullable = false)
    val antall: BigDecimal = BigDecimal(0),

    @Column(name = "enhetspris_per_maned", nullable = false)
    val enhetsprisPerManed: BigDecimal = BigDecimal(0),

    @Column(name = "belop", nullable = false)
    val belop: BigDecimal = BigDecimal(0),
) {
    override fun toString(): String {
        return "beskrivelse: $beskrivelse, belop: $belop"
    }
}
