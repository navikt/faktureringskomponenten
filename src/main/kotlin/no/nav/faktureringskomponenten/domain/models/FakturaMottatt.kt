package no.nav.faktureringskomponenten.domain.models

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "faktura_mottatt")
class FakturaMottatt(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "faktura_referanse_nr", nullable = false)
    val fakturaReferanseNr: String? = null,

    @Column(name = "faktura_nummer", nullable = true)
    val fakturaNummer: String? = null,

    @Column(name = "dato", nullable = false)
    val dato: LocalDate? = null,

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    val status: FakturaMottattStatus = FakturaMottattStatus.FEIL,

    @Column(name = "faktura_belop", nullable = true)
    val fakturaBelop: BigDecimal? = null,

    @Column(name = "ubetalt_belop", nullable = true)
    val ubetaltBelop: BigDecimal? = null,

    @Column(name = "feilmelding", nullable = true)
    val feilMelding: String? = null,
) {
}
