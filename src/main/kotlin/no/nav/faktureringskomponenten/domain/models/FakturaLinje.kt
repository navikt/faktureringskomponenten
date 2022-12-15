package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import javax.persistence.*

@Schema(
    description = "Model for en linje i fakturaen. Liste av linjer gir grunnlag for hele fakturabeløpet"
)
@Entity
@Table(name = "faktura_linje")
data class FakturaLinje(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,


    @Schema(
        description = "Startdato for perioden"
    )
    @Column(name = "periode_fra", nullable = false)
    val periodeFra: LocalDate,


    @Schema(
        description = "Sluttdato for perioden"
    )
    @Column(name = "periode_til", nullable = false)
    val periodeTil: LocalDate,


    @Schema(
        description = "Beskrivelse på hva grunnlaget for faktureringsbeløpet for perioden. Perioden blir automatisk lagt på",
    )
    @Column(name = "beskrivelse", nullable = false)
    val beskrivelse: String,


    @Schema(
        description = "Totalbeløp for hele fakturalinjen"
    )
    @Column(name = "belop", nullable = false)
    val belop: BigDecimal,


    @Schema(
        description = "Enhetspris per måned"
    )
    @Column(name = "enhetspris_per_maned", nullable= false)
    val enhetsprisPerManed: BigDecimal
)
