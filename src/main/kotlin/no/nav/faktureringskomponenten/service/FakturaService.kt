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

    @Transactional
    fun hentFakturaerMedStatus(status: FakturaStatus): List<Faktura> {
        return fakturaRepository.findByStatus(status)
    }

    fun hentFakturaerForFakturaserie(fakturaserieReferanse: String): List<Faktura> =
        fakturaRepository.findByFakturaserieReferanse(fakturaserieReferanse)

    /**
     * Setter status på samtlige fakturaer i en fakturaserie. Returnerer fakturaene som faktisk ble endret,
     * altså de som ikke allerede hadde [nyStatus].
     */
    @Transactional
    fun oppdaterStatusForAlleFakturaerIFakturaserie(
        fakturaserieReferanse: String,
        nyStatus: FakturaStatus
    ): List<Faktura> {
        val fakturaerSomSkalEndres = fakturaRepository.findByFakturaserieReferanse(fakturaserieReferanse)
            .filter { it.status != nyStatus }

        log.info {
            "Oppdaterer status til $nyStatus på ${fakturaerSomSkalEndres.size} fakturaer " +
                "for fakturaserie $fakturaserieReferanse"
        }

        fakturaerSomSkalEndres.forEach { it.status = nyStatus }
        return fakturaRepository.saveAll(fakturaerSomSkalEndres)
    }
}
