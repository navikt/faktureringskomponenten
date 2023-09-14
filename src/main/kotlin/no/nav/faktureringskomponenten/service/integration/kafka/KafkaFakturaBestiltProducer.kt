package no.nav.faktureringskomponenten.service.integration.kafka

import mu.KotlinLogging
import no.nav.faktureringskomponenten.config.MDCOperations
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }

@Component
class KafkaFakturaBestiltProducer(
    @Value("\${kafka.producer.faktura-bestilt}") private val topicName: String,
    @Qualifier("fakturaBestilt") @Autowired
    private val kafkaTemplate: KafkaTemplate<String, FakturaBestiltDto>
) : FakturaBestiltProducer {

    override fun produserBestillingsmelding(fakturaBestiltDto: FakturaBestiltDto) {
        val fakturaBestiltRecord = ProducerRecord<String, FakturaBestiltDto>(topicName, fakturaBestiltDto)
        fakturaBestiltRecord.headers().add(MDCOperations.CORRELATION_ID, MDCOperations.correlationId.encodeToByteArray())
        val future = kafkaTemplate.send(fakturaBestiltRecord)

        try {
            val sendeResultat = future.get(15L, TimeUnit.SECONDS)
            log.info(
                "Melding sendt på topic $topicName " +
                        "for referanse ${fakturaBestiltDto.fakturaserieReferanse}. " +
                        "Offset: ${sendeResultat.recordMetadata.offset()} "
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Avbrutt ved sending av melding om faktura bestilt for referanse ${fakturaBestiltDto.fakturaserieReferanse}")
        } catch (e: Exception) {
            throw RuntimeException(
                "Kunne ikke sende melding om faktura bestilt for referanse ${fakturaBestiltDto.fakturaserieReferanse}", e
            )
        }
    }
}
