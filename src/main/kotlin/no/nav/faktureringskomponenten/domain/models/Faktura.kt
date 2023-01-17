package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import no.nav.faktureringskomponenten.domain.converter.FakturaStatusConverter
import no.nav.faktureringskomponenten.domain.type.EnumTypePostgreSql
import org.hibernate.annotations.JdbcType
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Type
import org.hibernate.type.NumericBooleanConverter
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType
import org.springframework.cglib.core.Local
import java.time.LocalDate
import kotlin.jvm.Transient
import kotlin.reflect.typeOf

@Schema(description = "Model for en faktura i fakturaserien")
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


    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FakturaStatus = FakturaStatus.OPPRETTET,


    @Schema(
        description = "Fakturalinjer i fakturaen"
    )
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "faktura_id", nullable = false)
    val fakturaLinje: List<FakturaLinje>
) {

    @ManyToOne
    @JoinColumn(name = "fakturaserie_id", nullable = false, insertable = false, updatable = false)
    @Transient
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

    @Override
    override fun toString(): String {
        return "datoBestilt: $datoBestilt, status: $status, fakturaLinje: $fakturaLinje"
    }

    constructor() : this(
        id = null,
        datoBestilt = LocalDate.now(),
        status = FakturaStatus.OPPRETTET,
        fakturaLinje = listOf()
    )
}