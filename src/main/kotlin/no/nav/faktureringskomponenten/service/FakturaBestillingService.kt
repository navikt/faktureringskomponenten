package no.nav.faktureringskomponenten.service

import io.micrometer.core.instrument.Metrics
import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
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
        val feiledeFaktura = fakturaRepository.findAllByFakturaSomTrengerRekjøring()
        val bestillingsklareFaktura =
            fakturaRepository.findAllByDatoBestiltIsLessThanEqualAndStatusIsOpprettet(bestillingsDato)
        return bestillingsklareFaktura + feiledeFaktura
    }


    @Transactional
    fun bestillFaktura(fakturaReferanseNr: String) {
        val faktura = fakturaRepository.findByReferanseNr(fakturaReferanseNr) ?: throw RessursIkkeFunnetException(
            field = "fakturaReferanseNr",
            message = "Finner ikke faktura med faktura referanse nr $fakturaReferanseNr"
        )

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

        fakturaserieRepository.save(fakturaserie)
        fakturaRepository.save(faktura)

        fakturaBestiltProducer.produserBestillingsmelding(fakturaBestiltDto)
        Metrics.counter(MetrikkNavn.FAKTURA_BESTILT).increment()
    }

    @Transactional
    fun bestillKreditnota(fakturaserieReferanse: String) {
        val fakturaserie =
            fakturaserieRepository.findByReferanse(fakturaserieReferanse) ?: throw RessursIkkeFunnetException(
                field = "fakturaserieId",
                message = "Finner ikke fakturaserie med referanse $fakturaserieReferanse"
            )

        fakturaserie.faktura.forEach {
            fakturaBestiltProducer.produserBestillingsmelding(
                FakturaBestiltDtoMapper().tilFakturaBestiltDto(
                    it,
                    fakturaserie
                )
            )
            Metrics.counter(MetrikkNavn.FAKTURA_BESTILT).increment()
        }

        fakturaserie.apply {
            status = FakturaserieStatus.FERDIG
            faktura.map {
                it.status = FakturaStatus.BESTILT
            }
        }

        fakturaserieRepository.save(fakturaserie)
    }
}
