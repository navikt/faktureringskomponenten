package no.nav.faktureringskomponenten.service.integration.kafka

import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.faktureringskomponenten.service.FakturaService
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.listener.AbstractConsumerSeekAware
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback
import org.springframework.stereotype.Component

@Component
class FakturaMottattConsumer(
    private val fakturaService: FakturaService,
    private val listenerContainer: KafkaListenerEndpointRegistry,
    private val fakturaMotakFeilRepository: FakturaMottakFeilRepository
) : AbstractConsumerSeekAware() {

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
            log.error(
                "Feil ved mottak av faktura kafka melding - stopping container\n" +
                        "offset=${consumerRecord.offset()}\n" +
                        "Error:${e.message}", e
            )
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

    fun start() {
        listenerContainer.start()
    }

    fun stop() {
        listenerContainer.stop()
    }

    fun settSpesifiktOffsetPåConsumer(offset: Long) {
        log.info("settSpesifiktOffsetPåConsumer til $offset")
        seekCallbacks.forEach { (tp: TopicPartition, callback: ConsumerSeekCallback) ->
            log.info("tp:${tp.topic()} seek to:$offset")
            callback.seek(tp.topic(), tp.partition(), offset)
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FakturaMottattConsumer::class.java)
    }
}
