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

    @Column(name = "avregning_forrige_beloep")
    val avregningForrigeBeloep: BigDecimal? = null,

    @Column(name = "avregning_nytt_beloep")
    val avregningNyttBeloep: BigDecimal? = null,

    @Column(name = "belop", nullable = false)
    val belop: BigDecimal = BigDecimal(0),
) : BaseEntity() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FakturaLinje

        if (periodeFra != other.periodeFra) return false
        if (periodeTil != other.periodeTil) return false
        if (beskrivelse != other.beskrivelse) return false
        if (antall != other.antall) return false
        if (enhetsprisPerManed != other.enhetsprisPerManed) return false
        if (avregningForrigeBeloep != other.avregningForrigeBeloep) return false
        if (avregningNyttBeloep != other.avregningNyttBeloep) return false
        if (belop != other.belop) return false

        return true
    }

    override fun hashCode(): Int {
        var result = periodeFra.hashCode()
        result = 31 * result + periodeTil.hashCode()
        result = 31 * result + beskrivelse.hashCode()
        result = 31 * result + antall.hashCode()
        result = 31 * result + enhetsprisPerManed.hashCode()
        result = 31 * result + (avregningForrigeBeloep?.hashCode() ?: 0)
        result = 31 * result + (avregningNyttBeloep?.hashCode() ?: 0)
        result = 31 * result + belop.hashCode()
        return result
    }

    override fun toString(): String {
        return "Linje(periodeFra=$periodeFra, periodeTil=$periodeTil, beskrivelse='$beskrivelse', antall=$antall, enhetsprisPerManed=$enhetsprisPerManed," +
                (avregningForrigeBeloep?.let { " avregningForrigeBeloep=$avregningForrigeBeloep," } ?: "") +
                (avregningNyttBeloep?.let { " avregningNyttBeloep=$avregningNyttBeloep," } ?: "") +
                " belop=$belop)"
    }
}
