package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottattRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import no.nav.faktureringskomponenten.service.mappers.FakturaMottattMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Component
class FakturaMottattService(
    private val fakturaRepository: FakturaRepository,
    private val fakturaMottattMapper: FakturaMottattMapper,
    private val fakturaMottattRepository: FakturaMottattRepository
) {

    @Transactional
    fun lagreFakturaMottattMelding(fakturaMottattDto: FakturaMottattDto) {
        log.info("Mottatt $fakturaMottattDto")

        fakturaRepository.findById(fakturaMottattDto.fakturaReferanseNr.toLong())
            ?: throw RessursIkkeFunnetException(
                field = "fakturaId",
                message = "Finner ikke faktura med faktura id $fakturaMottattDto.fakturaReferanseNr"
            )

        val fakturaMottatt = fakturaMottattMapper.tilFakturaMottat(fakturaMottattDto);

        log.info("Mappet til modell og lagrer $fakturaMottattDto")
        fakturaMottattRepository.save(fakturaMottatt)
    }
}