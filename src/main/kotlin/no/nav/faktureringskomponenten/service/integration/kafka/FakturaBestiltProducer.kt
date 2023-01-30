package no.nav.faktureringskomponenten.service.integration.kafka

import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class FakturaBestiltProducer(
    @Value("\${kafka.producer.faktura-bestilt}") private val topicName: String,
    @Qualifier("fakturaBestilt") @Autowired
    private val kafkaTemplate: KafkaTemplate<String, FakturaBestiltDto>
) {
    private val log: Logger = LoggerFactory.getLogger(FakturaBestiltProducer::class.java)

    fun produserBestillingsmelding(fakturaBestiltDto: FakturaBestiltDto) {

        val producerRecord: ProducerRecord<String, FakturaBestiltDto> =
            ProducerRecord<String, FakturaBestiltDto>(topicName, fakturaBestiltDto)

        log.info("Prøver å sende melding sendt på topic $topicName \n$producerRecord")

        val future = kafkaTemplate.send(producerRecord)


        try {
            val sendeResultat = future.get(15L, TimeUnit.SECONDS)
            log.info(
                "Melding sendt på topic $topicName " +
                        "for vedtaksId ${fakturaBestiltDto.vedtaksId}. " +
                        "Offset: ${sendeResultat.recordMetadata.offset()} "
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Avbrutt ved sending av melding om faktura bestilt for vedtaksId ${fakturaBestiltDto.vedtaksId}")
        } catch (e: Exception) {
            throw RuntimeException(
                "Kunne ikke sende melding om faktura bestilt for vedtaksId ${fakturaBestiltDto.vedtaksId}", e
            )

        }
    }
}