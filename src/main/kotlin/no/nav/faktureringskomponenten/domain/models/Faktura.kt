package no.nav.faktureringskomponenten.domain.models

import java.time.LocalDate
import javax.persistence.*

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
    val status: FakturaStatus = FakturaStatus.OPPRETTET,

    @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name="faktura_id", nullable = false)
    val fakturaLinje: List<FakturaLinje>
)