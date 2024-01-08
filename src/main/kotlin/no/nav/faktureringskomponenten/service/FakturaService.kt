package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import org.springframework.stereotype.Component


@Component
class FakturaService(val fakturaRepository: FakturaRepository) {

    fun hentAntallFeiledeFakturaer(): Int {
        return fakturaRepository.countByStatusIsFeil()
    }
}