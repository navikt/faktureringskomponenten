package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate


@Component
class FakturaService(
    @Autowired val fakturaRepository: FakturaRepository,
) {
    fun hentBestillingsklareFaktura(bestillingsDato: LocalDate = LocalDate.now()): List<Faktura> {
        return fakturaRepository.findAllByDatoBestiltIsLessThanEqualAndStatusIs(bestillingsDato)
    }

    fun bestillFaktura(fakturaId: Long) {
        val faktura = fakturaRepository.findById(fakturaId)
        faktura.status = FakturaStatus.BESTILLT
        fakturaRepository.save(faktura)
    }

//    fun hentFakturaForFakturaserie(fakturaserieId: Long): List<Faktura> {
//        return fakturaRepository.findAllByFakturaserieId(fakturaserieId)
//    }
}