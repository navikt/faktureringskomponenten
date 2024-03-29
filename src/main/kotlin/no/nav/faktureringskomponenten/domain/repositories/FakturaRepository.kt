package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.Faktura
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface FakturaRepository : JpaRepository<Faktura, String> {

    @Query("SELECT f FROM Faktura f WHERE f.datoBestilt <= ?1 AND f.status = 'OPPRETTET'")
    fun findAllByDatoBestiltIsLessThanEqualAndStatusIsOpprettet(bestiltDato: LocalDate): List<Faktura>

    @EntityGraph(attributePaths = ["fakturaLinje"])
    fun findByFakturaserieReferanse(fakturaserieRef: String): List<Faktura>

    fun findByReferanseNr(referanseNr: String): Faktura?

    @Query("SELECT COUNT(f) FROM Faktura f WHERE f.status = 'FEIL'")
    fun countByStatusIsFeil(): Int
}
