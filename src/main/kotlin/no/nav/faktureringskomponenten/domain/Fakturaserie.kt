package no.nav.faktureringskomponenten.domain

import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.*
import javax.validation.constraints.Max

@Entity
data class Fakturaserie(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(nullable = false)
    val vedtaksnummer: String,

    @Column(name="faktura_gjelder", nullable = false)
    @field:Max(240)
    val fakturaGjelder: String,

    @Column(name="start_dato", nullable = false)
    val startDato: LocalDate,

    @Column(name="slutt_dato", nullable = false)
    val sluttDato: LocalDate,

    @Column(name = "fakturaserie_status", nullable = false)
    val status: FakturaserieStatus,

    @Column(name = "fakturaserie_intervall", nullable = false)
    val intervall: FakturaserieIntervall,

    @Column(name = "opprettet_tidspunkt", nullable = false)
    val opprettetTidspunkt: LocalDateTime = LocalDateTime.now()
)