package no.nav.faktureringskomponenten.domain.models

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Column(name = "opprettet_tidspunkt", nullable = false, updatable = false)
    @CreatedDate
    internal var opprettetTidspunkt : Instant = Instant.now()
}

@MappedSuperclass
abstract class AuditableEntity : BaseEntity() {

    @Column(name = "opprettet_av", nullable = false, updatable = false)
    @CreatedBy
    internal var opprettetAv : String = ""
}

@MappedSuperclass
abstract class ModifiableEntity : AuditableEntity() {

    @Column(name = "endret_av")
    @LastModifiedBy
    internal var endretAv : String = ""

    @Column(name = "endret_tidspunkt")
    @LastModifiedDate
    internal var endretTidspunkt : Instant = Instant.now()
}
