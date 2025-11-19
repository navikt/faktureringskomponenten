package no.nav.faktureringskomponenten.domain.models

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "ekstern_faktura_status")
class EksternFakturaStatus(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "dato", nullable = false)
    val dato: LocalDate? = null,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    val status: FakturaStatus? = null,

    @Column(name = "faktura_belop", nullable = true)
    val fakturaBelop: BigDecimal? = null,

    @Column(name = "ubetalt_belop", nullable = true)
    val ubetaltBelop: BigDecimal? = null,

    @Column(name = "feilmelding", nullable = true)
    val feilMelding: String? = null,

    @Column(name = "sendt", nullable = true)
    var sendt: Boolean? = false,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faktura_id", nullable = false, insertable = false, updatable = false)
    var faktura: Faktura? = null
) : BaseEntity() {
    companion object
}
