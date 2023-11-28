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

    @Column(name="referanse_nr", nullable = false, unique = true)
    val referanseNr: String = "",

    @Column(name = "dato_bestilt", nullable = false)
    val datoBestilt: LocalDate = LocalDate.now(),

    @Column(name = "sist_oppdatert", nullable = false)
    var sistOppdatert: LocalDate = LocalDate.now(),

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FakturaStatus = FakturaStatus.OPPRETTET,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "faktura_id", nullable = false)
    val fakturaLinje: List<FakturaLinje> = mutableListOf(),

    @ManyToOne
    @JoinColumn(name = "fakturaserie_id", nullable = false, insertable = false, updatable = false)
    var fakturaserie: Fakturaserie? = null,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "faktura_id")
    var eksternFakturaStatus: MutableList<EksternFakturaStatus> = mutableListOf(),

    @Column(name="eksternt_fakturanummer", nullable = false, unique = true)
    var eksternFakturaNummer: String = "",

    @Column(name = "kredit_referanse_nr", nullable = true, unique = true)
    var kreditReferanseNr: String = "",
) : AuditableEntity() {

    override fun toString(): String {
        return "referanseNr: $referanseNr, datoBestilt: $datoBestilt, status: $status"
    }

    fun getLinesAsString(): String {
        return fakturaLinje.sortedBy(FakturaLinje::periodeFra).map(FakturaLinje::toString).reduce { acc, s ->  acc + "\n" + s}
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

    fun erAvregningsfaktura() : Boolean {
        return fakturaLinje.any { it.referertFakturaVedAvregning != null }
    }
}
