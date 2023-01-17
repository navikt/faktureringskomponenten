package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface FakturaRepository : JpaRepository<Faktura, String> {
    fun findAllByDatoBestiltIsLessThanEqual(bestiltDato: LocalDate): List<Faktura>

    fun findAllByDatoBestiltIsLessThanEqualAndStatusIs(
        bestiltDato: LocalDate,
        status: FakturaStatus = FakturaStatus.OPPRETTET
    ): List<Faktura>

    fun findById(id: Long): Faktura
}