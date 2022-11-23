package no.nav.faktureringskomponenten.domain.models

import java.time.LocalDate
import javax.persistence.*

@Entity
@Table(name = "faktura")
data class Faktura(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @JoinColumn(name="fakturaserie_id", nullable = false)
    @ManyToOne
    val fakturaserie: Fakturaserie,

    @Column(name = "dato_bestillt", nullable = false)
    val datoBestilt: LocalDate,

    @Column(nullable = false)
    val status: FakturaStatus = FakturaStatus.OPPRETTET,

    @Column(name = "dato_betalt", nullable = false)
    val beskrivelse: String,

    @OneToMany(mappedBy = "faktura", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val fakturaLinje: List<FakturaLinje>
)