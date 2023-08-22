package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.controller.validators.IkkeDuplikatVedtaksId
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaMottatt
import no.nav.faktureringskomponenten.domain.models.FakturaMottattStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottattRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.integration.kafka.ManglendeFakturabetalingProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.ManglendeFakturabetalingDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import no.nav.faktureringskomponenten.service.mappers.FakturaMottattMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Component
class FakturaMottattService(
    private val fakturaRepository: FakturaRepository,
    private val fakturaMottattMapper: FakturaMottattMapper,
    private val fakturaMottattRepository: FakturaMottattRepository,
    private val manglendeFakturabetalingProducer: ManglendeFakturabetalingProducer
) {

    fun hentFakturamottat(fakturaReferanseNr: String): List<FakturaMottatt>? = fakturaMottattRepository.findAllByFakturaReferanseNr(fakturaReferanseNr)

    @Transactional
    fun lagreFakturaMottattMelding(fakturaMottattDto: FakturaMottattDto) {
        log.info("Mottatt $fakturaMottattDto")

        val faktura = fakturaRepository.findById(fakturaMottattDto.fakturaReferanseNr.toLong())

        faktura ?: throw RessursIkkeFunnetException(
            field = "fakturaId",
            message = "Finner ikke faktura med faktura id $fakturaMottattDto.fakturaReferanseNr"
        )

        val fakturaMottatt = fakturaMottattMapper.tilFakturaMottat(fakturaMottattDto);

        if(faktura.fakturaserie?.vedtaksId != null) {
            try {
                if(fakturaMottatt.status == FakturaMottattStatus.MANGLENDE_BETALING) {
                    manglendeFakturabetalingProducer.produserBestillingsmelding(
                        ManglendeFakturabetalingDto(
                            vedtaksId = faktura.fakturaserie!!.vedtaksId,
                            mottaksDato = fakturaMottatt.dato!!
                        )
                    )
                    fakturaMottattRepository.save(fakturaMottatt.apply { sendt = true })
                } else {
                    fakturaMottattRepository.save(fakturaMottatt.apply { sendt = false })
                }

            } catch (e: Exception) {
                fakturaMottattRepository.save(fakturaMottatt.apply { sendt = false })
                throw RuntimeException(
                    "Kunne ikke produsere melding om faktura mottatt bestilt for behandlingsID ${faktura.fakturaserie!!.vedtaksId}", e
                )
            }
        }
    }
}