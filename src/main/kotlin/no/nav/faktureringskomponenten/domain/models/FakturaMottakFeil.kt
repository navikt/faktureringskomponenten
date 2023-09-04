package no.nav.faktureringskomponenten.domain.models

import jakarta.persistence.*

@Entity
@Table(name = "faktura_mottak_feil")
class FakturaMottakFeil(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "error")
    val error: String? = null,

    @Column(name = "kafka_melding")
    val kafkaMelding: String? = null,

    @Column(name = "referanse_id")
    val referanseId: String? = null,

    @Column(name = "faktura_referanse_nr")
    val fakturaReferanseNr: String? = null,

    @Column(name = "kafka_offset")
    val kafkaOffset: Long? = null,
) {
    override fun toString(): String {
        return "id= $id fakturaReferanseNr= $fakturaReferanseNr\nError= $error"
    }
}
