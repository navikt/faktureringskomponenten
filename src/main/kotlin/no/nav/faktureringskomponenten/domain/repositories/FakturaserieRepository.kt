package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface FakturaserieRepository : JpaRepository<Fakturaserie, String> {
    fun findByVedtaksId(vedtaksId: String): Optional<Fakturaserie>
}