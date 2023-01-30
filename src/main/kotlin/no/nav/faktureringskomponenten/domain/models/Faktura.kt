package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import java.time.LocalDate

@Schema(description = "Model for en faktura i fakturaserien")
@Entity
@Table(name = "faktura")
data class Faktura(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,


    @Schema(
        description = "Dato for når faktura bestilles til OEBS"
    )
    @Column(name = "dato_bestilt", nullable = false)
    val datoBestilt: LocalDate = LocalDate.now(),


    @Column(name = "status", nullable = false, columnDefinition = "enum('OPPRETTET','BESTILLT','KANSELLERT')")
    @Enumerated(EnumType.STRING)
    var status: FakturaStatus = FakturaStatus.OPPRETTET,


    @Schema(
        description = "Fakturalinjer i fakturaen"
    )
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "faktura_id", nullable = false)
    val fakturaLinje: List<FakturaLinje> = listOf(),

    @ManyToOne
    @JoinColumn(name = "fakturaserie_id", nullable = false, insertable = false, updatable = false)
    var fakturaserie: Fakturaserie? = null

) {
    @Schema(
        description = "Startdato for perioden"
    )
    fun getPeriodeFra(): LocalDate {
        return fakturaLinje.minOf { it.periodeFra }
    }

    @Schema(
        description = "Sluttdato for perioden"
    )
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Faktura
        return id == other.id
    }
    override fun hashCode(): Int {
        return id.hashCode()
    }
}
