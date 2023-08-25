package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FakturaserieRepository : JpaRepository<Fakturaserie, String> {

    fun findByVedtaksId(vedtaksId: String): Fakturaserie?

    @Query("SELECT DISTINCT fs FROM Fakturaserie fs JOIN FETCH fs.faktura f WHERE fs.vedtaksId LIKE :saksnummer% AND (:fakturaStatus IS NULL OR CAST(f.status AS String) = :fakturaStatus)")
    fun findAllFakturaserierWithFilteredFaktura(
        @Param("saksnummer") saksnummer: String,
        @Param("fakturaStatus") fakturaStatus: String?
    ): List<Fakturaserie>

    fun findById(id: Long): Fakturaserie?
}