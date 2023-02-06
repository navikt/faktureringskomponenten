package no.nav.faktureringskomponenten.service.integration.kafka

import no.nav.faktureringskomponenten.service.FakturaService
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class FakturaMottattConsumer(
    @Autowired val fakturaService: FakturaService
) {
    private val log: Logger = LoggerFactory.getLogger(FakturaMottattConsumer::class.java)

    @KafkaListener(
        clientIdPrefix = "melosys-faktureringskomponenten-fakturaMottatt",
        topics = ["\${kafka.consumer.oebs.topic}"],
        containerFactory = "faktarMottattHendelseListenerContainerFactory",
        groupId = "\${kafka.consumer.oebs.groupid}"
    )
    fun fakturaMottatt(consumerRecord: ConsumerRecord<String, FakturaMottattDto>) {
        log.info("Mottatt melding {}", consumerRecord)
        fakturaService.lagreFakturaMottattMelding(consumerRecord.value())
    }
}
