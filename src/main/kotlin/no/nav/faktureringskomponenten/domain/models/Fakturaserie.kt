package no.nav.faktureringskomponenten.domain.models

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "fakturaserie")
class Fakturaserie(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    val vedtaksId: String = "",

    @Column(name = "faktura_gjelder_innbetalingstype", nullable = false)
    @Enumerated(EnumType.STRING)
    val fakturaGjelderInnbetalingstype: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,

    @Column(name = "fodselsnummer", nullable = false)
    val fodselsnummer: String = "",

    @Embedded
    val fullmektig: Fullmektig? = null,

    @Column(name = "referanse_bruker", nullable = false)
    val referanseBruker: String = "",

    @Column(name = "referanse_nav", nullable = false)
    val referanseNAV: String = "",

    @Column(nullable = false)
    val startdato: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    val sluttdato: LocalDate = LocalDate.now(),

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: FakturaserieStatus = FakturaserieStatus.OPPRETTET,

    @Column(name = "intervall", nullable = false)
    @Enumerated(EnumType.STRING)
    val intervall: FakturaserieIntervall = FakturaserieIntervall.MANEDLIG,

    @Column(name = "opprettet_tidspunkt", nullable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "fakturaserie_id", nullable = false)
    val faktura: List<Faktura> = mutableListOf()
) {
    @Override
    override fun toString(): String {
        return "vedtaksId: $vedtaksId, " +
                "fakturaGjelderInnbetalingstype: $fakturaGjelderInnbetalingstype, " +
                "referanseNAV: $referanseNAV, " +
                "startdato: $startdato, " +
                "sluttDato: $sluttdato, " +
                "faktura: $faktura"
    }
}
