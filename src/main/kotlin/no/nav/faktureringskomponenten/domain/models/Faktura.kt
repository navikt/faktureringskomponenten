package no.nav.faktureringskomponenten.domain.models

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "faktura")
class Faktura(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "dato_bestilt", nullable = false)
    val datoBestilt: LocalDate = LocalDate.now(),

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FakturaStatus = FakturaStatus.OPPRETTET,

    @Column(name = "innbetalt_belop", nullable = false)
    var innbetaltBelop: BigDecimal = BigDecimal(0.0),

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "faktura_id", nullable = false)
    val fakturaLinje: List<FakturaLinje> = mutableListOf(),

    @ManyToOne
    @JoinColumn(name = "fakturaserie_id", nullable = false, insertable = false, updatable = false)
    var fakturaserie: Fakturaserie? = null,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "faktura_referanse_nr", nullable = false, insertable = false, updatable = false)
    val fakturaMottat: List<FakturaMottatt> = mutableListOf(),
) {

    override fun toString(): String {
        return "id: $id, datoBestilt: $datoBestilt, status: $status"
    }

    fun getPeriodeFra(): LocalDate {
        return fakturaLinje.minOf { it.periodeFra }
    }

    fun getPeriodeTil(): LocalDate {
        return fakturaLinje.maxOf { it.periodeTil }
    }

    fun getFakturaserieId(): Long?{
        return fakturaserie?.id
    }

    fun totalbeløp(): BigDecimal {
        return fakturaLinje.sumOf(FakturaLinje::belop)
    }
}
