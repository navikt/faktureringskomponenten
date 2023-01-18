package no.nav.faktureringskomponenten.domain.models

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime


@Schema(
    description = "Model for fakturaserie, inneholder informasjon for alle bestilte og planlagte fakturaer"
)
@Entity
@Table(name = "fakturaserie")
data class Fakturaserie(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,


    @Schema(
        description = "Unik identifikator som saksbehandlingssystemet kjenner igjen",
    )
    @Column(nullable = false, unique = true)
    val vedtaksId: String = "",


    @Schema(
        description = "Informasjon om hva bruker betaler",
    )
    @Column(name = "faktura_gjelder", nullable = false)
    val fakturaGjelder: String = "",


    @Schema(
        description = "Fødselsnummer for fakturamottaker, 11 siffer",
    )
    @Column(name = "fodselsnummer", nullable = false)
    val fodselsnummer: BigDecimal = BigDecimal(0),


    @Embedded
    val fullmektig: Fullmektig? = null,


    @Schema(
        description = "Referanse for bruker/mottaker",
    )
    @Column(name = "referanse_bruker", nullable = false)
    val referanseBruker: String  = "",


    @Schema(
        description = "Referanse for NAV",
    )
    @Column(name = "referanse_nav", nullable = false)
    val referanseNAV: String = "",


    @Schema(
        description = "Startdato for fakturaserie",
    )
    @Column(nullable = false)
    val startdato: LocalDate = LocalDate.now(),


    @Schema(
        description = "Sluttdato for fakturaserie",
    )
    @Column(nullable = false)
    val sluttdato: LocalDate = LocalDate.now(),


    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FakturaserieStatus = FakturaserieStatus.OPPRETTET,


    @Column(name = "intervall", nullable = false)
    @Enumerated(EnumType.STRING)
    val intervall: FakturaserieIntervall = FakturaserieIntervall.MANEDLIG,


    @Schema(
        description = "Tidspunkt for opprettelse av fakturaserien",
    )
    @Column(name = "opprettet_tidspunkt", nullable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),


    @Schema(
        description = "Liste over planlagte fakturaer",
    )
    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "fakturaserie_id", nullable = false)
    val faktura: List<Faktura> = listOf()
) {
    @Override
    override fun toString(): String {
        return "vedtaksId: $vedtaksId, " +
                "fakturaGjelder: $fakturaGjelder, " +
                "referanseNAV: $referanseNAV, " +
                "startdato: $startdato, " +
                "sluttDato: $sluttdato, " +
                "faktura: $faktura"
    }
}
