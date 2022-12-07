package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.Faktura
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface FakturaRepository : JpaRepository<Faktura, String> {
    fun findAllByDatoBestiltIsLessThanEqual(dato: LocalDate): List<Faktura>
    fun findById(id: Long): Faktura
}