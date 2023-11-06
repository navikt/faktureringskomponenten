package no.nav.faktureringskomponenten.service.integration.kafka

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import mu.KotlinLogging
import no.nav.faktureringskomponenten.exceptions.ExternalErrorException
import no.nav.faktureringskomponenten.metrics.MetrikkNavn
import no.nav.faktureringskomponenten.service.EksternFakturaStatusService
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.listener.AbstractConsumerSeekAware
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class EksternFakturaStatusConsumer(
    private val eksternFakturaStatusService: EksternFakturaStatusService,
    private val kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry
) : AbstractConsumerSeekAware() {

    var x = 0;

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
        } catch (e: ExternalErrorException) {
            log.error(
                "Feil ved mottak av faktura ved mottak av kafka melding\n" +
                        "offset=${consumerRecord.offset()}\n" +
                        "Error:${e.message}", e
            )

            Metrics.counter(MetrikkNavn.FEIL_FRA_EKSTERN, listOf(Tag.of("Faktura_referanse_nummer", eksternFakturaStatusDto.fakturaReferanseNr), Tag.of("feilmelding", eksternFakturaStatusDto.feilmelding!!))).increment()
            //Metrics.gauge(MetrikkNavn.FEIL_FRA_EKSTERN, listOf(Tag.of("Faktura_referanse_nummer", eksternFakturaStatusDto.fakturaReferanseNr), Tag.of("feilmelding", eksternFakturaStatusDto.feilmelding!!)), x++)
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

    fun eksternFakturaStatusListenerContainer(): MessageListenerContainer {
        return kafkaListenerEndpointRegistry.getListenerContainer("fakturaMottatt")!!
    }


    fun start() {
        eksternFakturaStatusListenerContainer().start()
    }

    fun stop() {
        eksternFakturaStatusListenerContainer().stop()
    }

    fun settSpesifiktOffsetPåConsumer(offset: Long) {
        log.info("settSpesifiktOffsetPåConsumer til $offset")
        seekCallbacks.forEach { (tp: TopicPartition, callback: ConsumerSeekAware.ConsumerSeekCallback) ->
            log.info("tp:${tp.topic()} seek to:$offset")
            callback.seek(tp.topic(), tp.partition(), offset)
        }
    }

}
