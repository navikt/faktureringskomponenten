package no.nav.faktureringskomponenten.service

import io.micrometer.core.instrument.Metrics
import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.metrics.MetrikkNavn
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.mappers.FakturaBestiltDtoMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Service
class FakturaBestillingService(
    private val fakturaRepository: FakturaRepository,
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaBestiltProducer: FakturaBestiltProducer,
) {

    fun hentBestillingsklareFaktura(bestillingsDato: LocalDate = LocalDate.now()): List<Faktura> {
        return fakturaRepository.findAllByDatoBestiltIsLessThanEqualAndStatusIsOpprettet(bestillingsDato)
    }


    /**
     * Bestiller fakturaen til OEBS. Raden låses, og fakturaen bestilles bare hvis den fortsatt har
     * status OPPRETTET, slik at samtidige kall (cronjobb, adminendepunkter) ikke sender den to ganger.
     *
     * @return true hvis fakturaen ble bestilt, false hvis den allerede var bestilt eller avbrutt
     */
    @Transactional
    fun bestillFaktura(fakturaReferanseNr: String): Boolean {
        log.info("Bestiller faktura med referanse nr $fakturaReferanseNr")
        val faktura = fakturaRepository.findByReferanseNrForUpdate(fakturaReferanseNr) ?: throw RessursIkkeFunnetException(
            field = "fakturaReferanseNr",
            message = "Finner ikke faktura med faktura referanse nr $fakturaReferanseNr"
        )

        if (faktura.status != FakturaStatus.OPPRETTET) {
            log.warn("Faktura med referanse nr $fakturaReferanseNr har status ${faktura.status} og bestilles ikke")
            return false
        }

        val fakturaserieId = faktura.getFakturaserieId()
            ?: throw RessursIkkeFunnetException(
                field = "fakturaserieId",
                message = "Finner ikke fakturaserie med faktura referanse nr ${faktura.referanseNr}"
            )

        val fakturaserie = fakturaserieRepository.findById(fakturaserieId) ?: throw RessursIkkeFunnetException(
            field = "fakturaserieId",
            message = "Finner ikke fakturaserie med fakturaserieId $fakturaserieId"
        )

        faktura.status = FakturaStatus.BESTILT
        fakturaserie.status = FakturaserieStatus.UNDER_BESTILLING

        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(faktura, fakturaserie)
        faktura.beskrivelse = fakturaBestiltDto.beskrivelse

        fakturaserieRepository.save(fakturaserie)
        fakturaRepository.save(faktura)

        fakturaBestiltProducer.produserBestillingsmelding(fakturaBestiltDto)
        Metrics.counter(MetrikkNavn.FAKTURA_BESTILT).increment()
        return true
    }

    @Transactional
    fun bestillKreditnota(fakturaserie: Fakturaserie, beskrivelse: String) {
        fakturaserie.apply {
            status = FakturaserieStatus.FERDIG
            faktura.forEach {
                it.status = FakturaStatus.BESTILT
            }
        }

        fakturaserie.faktura.forEach {
            val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(it, fakturaserie, beskrivelse)
            it.beskrivelse = fakturaBestiltDto.beskrivelse

            fakturaBestiltProducer.produserBestillingsmelding(fakturaBestiltDto)
            Metrics.counter(MetrikkNavn.FAKTURA_BESTILT).increment()
        }
    }
}
