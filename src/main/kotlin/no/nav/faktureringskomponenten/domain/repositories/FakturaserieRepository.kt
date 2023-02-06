package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import org.springframework.data.jpa.repository.JpaRepository

interface FakturaserieRepository : JpaRepository<Fakturaserie, String> {

    fun findByVedtaksId(vedtaksId: String): Fakturaserie?

    fun findById(id: Long): Fakturaserie?
}