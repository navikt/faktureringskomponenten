package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface FakturaRepository : JpaRepository<Faktura, String> {
    fun findAllByDatoBestiltIsLessThanEqualAndStatusIs(
        bestiltDato: LocalDate,
        status: FakturaStatus = FakturaStatus.OPPRETTET
    ): List<Faktura>

//    @Query("SELECT f FROM faktura f WHERE fakturaserie_id = :fakturaserieId")
//    fun findAllByFakturaserieId(@Param("fakturaserieId") fakturaserieId: Long): List<Faktura>

    fun findById(id: Long): Faktura
}