package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import org.springframework.data.repository.CrudRepository
import org.springframework.transaction.annotation.Transactional

@Transactional
internal interface FakturaserieRepository: CrudRepository<Fakturaserie, String>{
    fun findByVedtaksId(vedtaksId: String)
}