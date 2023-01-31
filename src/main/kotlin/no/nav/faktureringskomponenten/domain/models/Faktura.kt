package no.nav.faktureringskomponenten.domain.models

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "faktura")
class Faktura(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "dato_bestilt", nullable = false)
    val datoBestilt: LocalDate = LocalDate.now(),

    @Column(name = "status", nullable = false, columnDefinition = "enum('OPPRETTET','BESTILLT','KANSELLERT')")
    @Enumerated(EnumType.STRING)
    var status: FakturaStatus = FakturaStatus.OPPRETTET,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "faktura_id", nullable = false)
    val fakturaLinje: List<FakturaLinje> = listOf(),

    @ManyToOne
    @JoinColumn(name = "fakturaserie_id", nullable = false, insertable = false, updatable = false)
    var fakturaserie: Fakturaserie? = null

) {
    fun getPeriodeFra(): LocalDate {
        return fakturaLinje.minOf { it.periodeFra }
    }

    fun getPeriodeTil(): LocalDate {
        return fakturaLinje.maxOf { it.periodeTil }
    }

    fun getFakturaserieId(): Long?{
        return fakturaserie?.id
    }

    @Override
    override fun toString(): String {
        return "$id: datoBestilt: $datoBestilt, status: $status, fakturaLinje: $fakturaLinje"
    }
}
