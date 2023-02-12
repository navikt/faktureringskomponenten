package no.nav.faktureringskomponenten.config

import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.common.errors.RecordDeserializationException
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer

class ContainerStoppingFailedJsonAwareErrorHandler(
    private val valueDeserializer: DeserializerFailedJsonAware<*>,
    private val fakturaMotakFeilRepository: FakturaMottakFeilRepository
) : CommonContainerStoppingErrorHandler() {

    override fun handleOtherException(
        thrownException: java.lang.Exception,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        batchListener: Boolean
    ) {
        val failedJson = valueDeserializer.getFailedJson()
        if (failedJson != null) {
            val recordDeserializationException = thrownException as? RecordDeserializationException
            fakturaMotakFeilRepository.saveAndFlush(
                FakturaMottakFeil(
                    error = thrownException.message,
                    kafkaMelding = failedJson,
                    kafkaOffset = recordDeserializationException?.offset()
                )
            )
        }
        super.handleOtherException(thrownException, consumer, container, batchListener)
    }
}
