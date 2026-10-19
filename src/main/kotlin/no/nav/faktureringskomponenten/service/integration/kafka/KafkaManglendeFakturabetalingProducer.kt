package no.nav.faktureringskomponenten.service.integration.kafka

import mu.KotlinLogging
import no.nav.faktureringskomponenten.service.integration.kafka.dto.ManglendeFakturabetalingDto
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }

@Component
class KafkaManglendeFakturabetalingProducer(
    @Value("\${kafka.producer.manglende-fakturabetaling}") private val topicName: String,
    @Qualifier("manglendeFakturabetaling") @Autowired
    private val kafkaTemplate: KafkaTemplate<String, ManglendeFakturabetalingDto>
) : ManglendeFakturabetalingProducer {

    override fun produserBestillingsmelding(manglendeFakturabetalingDto: ManglendeFakturabetalingDto) {
        val manglendeInnbetalingRecord = ProducerRecord<String, ManglendeFakturabetalingDto>(topicName, manglendeFakturabetalingDto)
        val future = kafkaTemplate.send(manglendeInnbetalingRecord)

        try {
            val sendeResultat = future.get(15L, TimeUnit.SECONDS)
            log.info(
                "Melding sendt på topic $topicName " +
                        "for saksnummer-behandlingsID ${manglendeFakturabetalingDto.fakturaserieReferanse}. " +
                        "Offset: ${sendeResultat.recordMetadata.offset()} "
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Avbrutt ved sending av melding om faktura bestilt for referanse ${manglendeFakturabetalingDto.fakturaserieReferanse}")
        } catch (e: Exception) {
            throw RuntimeException(
                "Kunne ikke sende melding om faktura bestilt for referanse ${manglendeFakturabetalingDto.fakturaserieReferanse}", e
            )
        }
    }
}