package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import org.springframework.stereotype.Component

@Component
class FakturaService(
    private val fakturaRepository: FakturaRepository,
) {

    fun hentFaktura(fakturaReferanseNr: String) = fakturaRepository.findByReferanseNr(fakturaReferanseNr);
}