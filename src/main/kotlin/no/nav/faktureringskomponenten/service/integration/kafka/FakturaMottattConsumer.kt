package no.nav.faktureringskomponenten.service.integration.kafka

import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.faktureringskomponenten.service.FakturaService
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.stereotype.Component

@Component
class FakturaMottattConsumer(
    private val fakturaService: FakturaService,
    private val listenerContainer: KafkaListenerEndpointRegistry,
    private val fakturaMotakFeilRepository: FakturaMottakFeilRepository
) {
    private val log: Logger = LoggerFactory.getLogger(FakturaMottattConsumer::class.java)

    @KafkaListener(
        clientIdPrefix = "melosys-faktureringskomponenten-fakturaMottatt",
        topics = ["\${kafka.consumer.oebs.topic}"],
        containerFactory = "faktarMottattHendelseListenerContainerFactory",
        groupId = "\${kafka.consumer.oebs.groupid}"
    )
    fun fakturaMottatt(consumerRecord: ConsumerRecord<String, FakturaMottattDto>) {
        val fakturaMottattDto = consumerRecord.value()
        log.info("Mottatt melding {}", consumerRecord)
        try {
            fakturaService.lagreFakturaMottattMelding(fakturaMottattDto)
        } catch (e: Exception) {
            log.error("Feil ved mottak av faktura kafka melding - stopping container\nError:${e.message}", e)
            stop()

            fakturaMotakFeilRepository.saveAndFlush(
                FakturaMottakFeil(
                    error = e.message,
                    kafkaOffset = consumerRecord.offset(),
                    vedtaksId = fakturaMottattDto.vedtaksId,
                    fakturaReferanseNr = fakturaMottattDto.fakturaReferanseNr
                )
            )
        }
    }

    fun stop() {
        listenerContainer.stop()
    }
}
