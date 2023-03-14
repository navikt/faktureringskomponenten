package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import no.nav.faktureringskomponenten.service.mappers.FakturaBestiltDtoMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Component
class FakturaService(
    private val fakturaRepository: FakturaRepository,
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaBestiltProducer: FakturaBestiltProducer,
) {

    fun hentBestillingsklareFaktura(bestillingsDato: LocalDate = LocalDate.now()): List<Faktura> =
        fakturaRepository.findAllByDatoBestiltIsLessThanEqualAndStatusIsOpprettet(bestillingsDato)

    fun lagreFakturaMottattMelding(fakturaMottattDto: FakturaMottattDto) {
        val faktura = fakturaRepository.findById(fakturaMottattDto.fakturaReferanseNr.toLong())
            ?: throw RessursIkkeFunnetException(
                field = "fakturaId",
                message = "Finner ikke faktura med faktura id $fakturaMottattDto.fakturaReferanseNr"
            )

        if (faktura.status == FakturaStatus.BESTILLT) {
            faktura.apply {
                status = fakturaMottattDto.status
                innbetaltBelop = fakturaMottattDto.belop
            }

            fakturaRepository.save(faktura)
            log.info("Faktura {} er endret til {}", faktura.id, faktura)
        } else {
            throw IllegalStateException("Faktura melding mottatt fra oebs med status: ${faktura.status}")
        }
    }

    @Transactional
    fun bestillFaktura(fakturaId: Long) {
        val faktura = fakturaRepository.findById(fakturaId) ?: throw RessursIkkeFunnetException(
            field = "fakturaId",
            message = "Finner ikke faktura med faktura id $fakturaId"
        )

        val fakturaserieId = faktura.getFakturaserieId()
            ?: throw RessursIkkeFunnetException(
                field = "fakturaserieId",
                message = "Finner ikke fakturaserie med faktura id ${faktura.id}"
            )

        val fakturaserie = fakturaserieRepository.findById(fakturaserieId) ?: throw RessursIkkeFunnetException(
            field = "fakturaserieId",
            message = "Finner ikke fakturaserie med fakturaserieId $fakturaserieId"
        )

        faktura.status = FakturaStatus.BESTILLT
        fakturaserie.status = FakturaserieStatus.UNDER_BESTILLING

        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(faktura, fakturaserie)

        fakturaserieRepository.save(fakturaserie)
        fakturaRepository.save(faktura)

        fakturaBestiltProducer.produserBestillingsmelding(fakturaBestiltDto)
    }
}
