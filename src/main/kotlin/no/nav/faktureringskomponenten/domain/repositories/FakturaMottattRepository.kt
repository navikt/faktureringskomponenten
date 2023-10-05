package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.FakturaMottatt
import no.nav.faktureringskomponenten.domain.models.FakturaMottattStatus
import org.springframework.data.jpa.repository.JpaRepository

interface FakturaMottattRepository : JpaRepository<FakturaMottatt, String> {

    fun findById(id: Long): FakturaMottatt?

    fun findAllByFakturaReferanseNr(id: Int): List<FakturaMottatt>?
}
