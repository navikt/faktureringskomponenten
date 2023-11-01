package no.nav.faktureringskomponenten.domain.models

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "faktura_mottak_feil")
class FakturaMottakFeil(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "error")
    val error: String? = null,

    @Column(name = "error_type")
    @Enumerated(EnumType.STRING)
    val errorType: ErrorTypes? = null,

    @Column(name = "kafka_melding")
    val kafkaMelding: String? = null,

    @Column(name = "faktura_referanse_nr")
    val fakturaReferanseNr: String? = null,

    @Column(name = "kafka_offset")
    val kafkaOffset: Long? = null,

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()

) {
    override fun toString(): String {
        return "id= $id fakturaReferanseNr= $fakturaReferanseNr\nError= $error"
    }
}
