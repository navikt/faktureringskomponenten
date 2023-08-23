package no.nav.faktureringskomponenten.service.integration.kafka

import mu.KotlinLogging
import no.nav.faktureringskomponenten.service.FakturaMottattService
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.listener.AbstractConsumerSeekAware
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class FakturaMottattConsumer(
    private val fakturaMottattService: FakturaMottattService,
    private val kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry
) : AbstractConsumerSeekAware() {

    @KafkaListener(
        id = "fakturaMottatt",
        clientIdPrefix = "melosys-faktureringskomponenten-fakturaMottatt",
        topics = ["\${kafka.consumer.oebs.topic}"],
        containerFactory = "fakturaMottattHendelseListenerContainerFactory",
        groupId = "\${kafka.consumer.oebs.groupid}"
    )
    fun fakturaMottatt(consumerRecord: ConsumerRecord<String, FakturaMottattDto>) {
        val fakturaMottattDto = consumerRecord.value()
        log.info("Mottatt melding {}", consumerRecord)
        try {
            fakturaMottattService.lagreFakturaMottattMelding(fakturaMottattDto)
        } catch (e: Exception) {
            log.error(
                "Feil ved lagring av faktura ved mottak av kafka melding\n" +
                        "offset=${consumerRecord.offset()}\n" +
                        "Error:${e.message}", e
            )
            throw FakturaMottattConsumerException(
                "Feil ved lagring av faktura: ${fakturaMottattDto.fakturaReferanseNr}",
                consumerRecord.offset(), e
            )
        }
    }

    fun fakturaMottattListenerContainer(): MessageListenerContainer {
        return kafkaListenerEndpointRegistry.getListenerContainer("fakturaMottatt")!!
    }

}
