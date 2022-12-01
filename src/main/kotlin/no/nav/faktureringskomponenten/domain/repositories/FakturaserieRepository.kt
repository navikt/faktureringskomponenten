package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Transactional
interface FakturaserieRepository : CrudRepository<Fakturaserie, String> {
    fun findByVedtaksId(vedtaksId: String): Optional<Fakturaserie>
}