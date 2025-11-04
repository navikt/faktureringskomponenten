package no.nav.faktureringskomponenten.domain.models

import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "fakturaserie")
class Fakturaserie(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "referanse", nullable = false, unique = true)
    val referanse: String = "",

    @Column(name = "faktura_gjelder_innbetalingstype", nullable = false)
    @Enumerated(EnumType.STRING)
    val fakturaGjelderInnbetalingstype: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,

    @Column(name = "fodselsnummer", nullable = false)
    val fodselsnummer: String = "",

    @Embedded
    var fullmektig: Fullmektig? = null,

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

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "fakturaserie_id", nullable = false)
    val faktura: List<Faktura> = mutableListOf(),

    @OneToOne(cascade = [CascadeType.REMOVE], fetch = FetchType.LAZY)
    @JoinColumn(name = "erstattet_med", referencedColumnName = "id")
    var erstattetMed: Fakturaserie? = null,

    ) : ModifiableEntity() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Fakturaserie

        if (id != other.id) return false
        if (referanse != other.referanse) return false

        return true
    }

    override fun hashCode(): Int {
        return referanse.hashCode()
    }

    override fun toString(): String {
        return "referanse: $referanse, " +
            "fakturaGjelderInnbetalingstype: $fakturaGjelderInnbetalingstype, " +
            "referanseNAV: $referanseNAV, " +
            "startdato: $startdato, " +
            "sluttDato: $sluttdato, " +
            "faktura: $faktura"
    }

    fun erAktiv(): Boolean {
        return status == FakturaserieStatus.OPPRETTET || status == FakturaserieStatus.UNDER_BESTILLING
    }

    fun erUnderBestilling(): Boolean {
        return status == FakturaserieStatus.UNDER_BESTILLING
    }

    fun erstattMed(nyFakturaserie: Fakturaserie) {
        avbrytPlanlagteFakturaer()
        erstattetMed = nyFakturaserie
        status = FakturaserieStatus.ERSTATTET
    }

    fun kansellerMed(nyFakturaserie: Fakturaserie) {
        avbrytPlanlagteFakturaer()
        erstattetMed = nyFakturaserie
        status = FakturaserieStatus.KANSELLERT
    }

    fun bestilteFakturaer(): List<Faktura> {
        return faktura.filter(Faktura::erBestilt)
    }

    fun planlagteFakturaer(): List<Faktura> {
        return faktura.filter { it.status == FakturaStatus.OPPRETTET }
    }

    fun avbrytPlanlagteFakturaer() {
        planlagteFakturaer().forEach { it.status = FakturaStatus.AVBRUTT }
    }

}
