package no.nav.faktureringskomponenten.domain.models

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    @CreatedDate
    internal var opprettetTidspunkt : Instant = Instant.now()
}