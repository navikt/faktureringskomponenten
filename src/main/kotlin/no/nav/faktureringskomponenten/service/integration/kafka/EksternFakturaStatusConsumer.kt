package no.nav.faktureringskomponenten.service.integration.kafka

import mu.KotlinLogging
import no.nav.faktureringskomponenten.service.EksternFakturaStatusService
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.listener.AbstractConsumerSeekAware
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class EksternFakturaStatusConsumer(
    private val eksternFakturaStatusService: EksternFakturaStatusService,
    private val kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry
) : AbstractConsumerSeekAware() {

    @KafkaListener(
        id = "fakturaMottatt",
        clientIdPrefix = "melosys-faktureringskomponenten-fakturaMottatt",
        topics = ["\${kafka.consumer.oebs.topic}"],
        containerFactory = "fakturaMottattHendelseListenerContainerFactory",
        groupId = "\${kafka.consumer.oebs.groupid}"
    )
    fun eksternFakturaStatus(consumerRecord: ConsumerRecord<String, EksternFakturaStatusDto>) {
        val eksternFakturaStatusDto = consumerRecord.value()
        log.info("Mottatt melding {}", consumerRecord)
        try {
            eksternFakturaStatusService.lagreEksternFakturaStatusMelding(eksternFakturaStatusDto)
        } catch (e: Exception) {
            log.error(
                "Feil ved lagring av faktura ved mottak av kafka melding\n" +
                        "offset=${consumerRecord.offset()}\n" +
                        "Error:${e.message}", e
            )
            throw EksternFakturaStatusConsumerException(
                "Feil ved lagring av faktura: ${eksternFakturaStatusDto.fakturaReferanseNr}",
                consumerRecord.offset(), e
            )
        }
    }

    fun EksternFakturaStatusListenerContainer(): MessageListenerContainer {
        return kafkaListenerEndpointRegistry.getListenerContainer("fakturaMottatt")!!
    }

}
