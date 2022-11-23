package no.nav.faktureringskomponenten.domain.models

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.Max

@Entity
@Table(name = "fakturaserie")
data class Fakturaserie(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(nullable = false, unique = true)
    val vedtaksId: String,

    @Column(name = "faktura_gjelder", nullable = false)
    @field:Max(240)
    val fakturaGjelder: String,

    @Column(name = "fodselsnummer", nullable = false)
    val fodselsnummer: BigDecimal,

    @Embedded
    val fullmektig: Fullmektig,

    @Column(name = "referanse_bruker", nullable = false)
    val referanseBruker: String,

    @Column(name = "referanse_nav", nullable = false)
    val referanseNAV: String,

    @Column(nullable = false)
    val startdato: LocalDate,

    @Column(nullable = false)
    val sluttdato: LocalDate,

    @Column(name = "status", nullable = false)
    val status: FakturaserieStatus = FakturaserieStatus.OPPRETTET,

    @Column(name = "intervall", nullable = false)
    val intervall: FakturaserieIntervall = FakturaserieIntervall.MANEDLIG,

    @Column(name = "opprettet_tidspunkt", nullable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "fakturaserie", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val faktura: List<Faktura>
)