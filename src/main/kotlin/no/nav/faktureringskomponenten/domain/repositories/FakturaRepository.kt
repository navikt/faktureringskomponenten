package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.Faktura
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface FakturaRepository : JpaRepository<Faktura, String> {
    @Query("SELECT f FROM Faktura f WHERE f.datoBestilt <= ?1 AND f.status = 'OPPRETTET'")
    fun findAllByDatoBestiltIsLessThanEqualAndStatusIsOpprettet(bestiltDato: LocalDate): List<Faktura>

    fun findById(id: Long): Faktura
}