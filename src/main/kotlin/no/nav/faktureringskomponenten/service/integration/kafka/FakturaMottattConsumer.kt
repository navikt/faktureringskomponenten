package no.nav.faktureringskomponenten.service.integration.kafka

import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class FakturaMottattConsumer(
    @Value("\${kafka.consumer.oebs.topic}") private val topic: String,
    @Value("\${kafka.consumer.oebs.groupid}") private val groupID: String,
) {
    private val log: Logger = LoggerFactory.getLogger(FakturaMottattConsumer::class.java)


    init {
        log.info("--- topic:{$topic}")
        log.info("--- groupID:{$groupID}")
    }

    @KafkaListener(
        clientIdPrefix = "melosys-faktureringskomponenten-fakturaMottatt",
        topics = ["\${kafka.consumer.oebs.topic}"],
        containerFactory = "faktarMottattHendelseListenerContainerFactory",
        groupId = "\${kafka.consumer.oebs.groupid}"
    )
    fun fakturaMottatt(consumerRecord: ConsumerRecord<String, FakturaMottattDto>) {
        log.info("Mottatt melding {}", consumerRecord)
    }
}