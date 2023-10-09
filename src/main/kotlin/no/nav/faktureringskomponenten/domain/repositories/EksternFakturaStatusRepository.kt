package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.EksternFakturaStatus
import org.springframework.data.jpa.repository.JpaRepository

interface EksternFakturaStatusRepository : JpaRepository<EksternFakturaStatus, String> {

    fun findById(id: Long): EksternFakturaStatus?

}
