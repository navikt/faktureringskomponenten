package no.nav.faktureringskomponenten.domain.models

import java.math.BigDecimal
import java.time.LocalDate
import javax.persistence.*
import javax.validation.constraints.Max

@Entity
@Table(name = "faktura_linje")
data class FakturaLinje(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @JoinColumn(name="faktura_id", nullable = false)
    @ManyToOne
    val faktura: Faktura,

    @Column(name="periode_fra", nullable = false)
    val periodeFra: LocalDate,

    @Column(name="periode_til", nullable = false)
    val periodeTil: LocalDate,

    @Column(name = "beskrivelse", nullable = false)
    @field:Max(240)
    val beskrivelse: String,

    @Column(name = "belop", nullable = false)
    val belop: BigDecimal
)