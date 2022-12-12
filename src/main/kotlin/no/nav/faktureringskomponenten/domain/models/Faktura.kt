package no.nav.faktureringskomponenten.domain.models

import no.nav.faktureringskomponenten.domain.type.EnumTypePostgreSql
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import java.time.LocalDate
import javax.persistence.*

@TypeDef(name = "enumType", typeClass = EnumTypePostgreSql::class)
@Entity
@Table(name = "faktura")
data class Faktura(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @Column(name = "dato_bestilt", nullable = false)
    val datoBestilt: LocalDate,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Type(type = "enumType")
    var status: FakturaStatus = FakturaStatus.OPPRETTET,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name="faktura_id", nullable = false)
    val fakturaLinje: List<FakturaLinje>
) {

    fun getPeriodeFra(): LocalDate {
        return fakturaLinje.minOf { it.periodeFra }
    }

    fun getPeriodeTil(): LocalDate {
        return fakturaLinje.maxOf { it.periodeTil }
    }
}