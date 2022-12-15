package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.faktureringskomponenten.domain.type.EnumTypePostgreSql
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.LocalDate
import javax.persistence.*

@Schema(
    description = "Model for en faktura i fakturaserien"
)
@TypeDef(name = "enumType", typeClass = EnumTypePostgreSql::class)
@Entity
@Table(name = "faktura")
data class Faktura(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,


    @Schema(
        description = "Dato for når faktura bestilles til OEBS"
    )
    @Column(name = "dato_bestilt", nullable = false)
    val datoBestilt: LocalDate,


    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Type(type = "enumType")
    var status: FakturaStatus = FakturaStatus.OPPRETTET,


    @Schema(
        description = "Fakturalinjer i fakturaen"
    )
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "faktura_id", nullable = false)
    val fakturaLinje: List<FakturaLinje>,
) {

    @ManyToOne
    @JoinColumn(name = "fakturaserie_id", nullable = false, insertable = false, updatable = false)
    var fakturaserie: Fakturaserie? = null


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
}