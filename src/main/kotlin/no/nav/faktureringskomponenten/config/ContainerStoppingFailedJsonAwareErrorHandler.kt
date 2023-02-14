package no.nav.faktureringskomponenten.config

import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.RecordDeserializationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
class ContainerStoppingFailedJsonAwareErrorHandler(
    private val valueDeserializer: DeserializerJsonAware

) : CommonContainerStoppingErrorHandler() {

    @Autowired
    private lateinit var fakturaMottakFeilRepository: FakturaMottakFeilRepository

    override fun handleRemaining(
        thrownException: Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer
    ) {
        saveError(thrownException)
        super.handleRemaining(thrownException, records, consumer, container)
    }

    override fun handleOtherException(
        thrownException: Exception,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        batchListener: Boolean
    ) {
        saveError(thrownException)
        super.handleOtherException(thrownException, consumer, container, batchListener)
    }

    private fun saveError(thrownException: Exception) {
        val failedJson = valueDeserializer.getJson()
        if (failedJson != null) {
            val recordDeserializationException = thrownException as? RecordDeserializationException
            fakturaMottakFeilRepository.saveAndFlush(
                FakturaMottakFeil(
                    error = thrownException.cause?.message ?: thrownException.message,
                    kafkaMelding = failedJson,
                    kafkaOffset = recordDeserializationException?.offset()
                )
            )
        }
    }
}
