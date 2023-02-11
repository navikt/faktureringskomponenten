package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import org.springframework.data.jpa.repository.JpaRepository

interface FakturaMottakFeilRepository : JpaRepository<FakturaMottakFeil, String> {

    fun findById(id: Long): FakturaMottakFeil?
}
