package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Component
class FakturaService(
    private val fakturaRepository: FakturaRepository,
) {

    fun hentFaktura(fakturaReferanseNr: String) = fakturaRepository.findByReferanseNr(fakturaReferanseNr)

    @Transactional
    fun lagreFaktura(faktura: Faktura): Faktura {
        return fakturaRepository.save(faktura)
    }

    fun hentAntallFeiledeFakturaer(): Int {
        return fakturaRepository.countByStatusIsFeil()
    }

    @Transactional
    fun oppdaterFakturaStatus(fakturaReferanseNr: String, nyStatus: FakturaStatus) {
        val faktura = fakturaRepository.findByReferanseNr(fakturaReferanseNr)
        log.info { "Oppdaterer faktura med referanse $fakturaReferanseNr fra status ${faktura?.status} til $nyStatus" }
        faktura?.status = nyStatus
    }
}
