package no.nav.faktureringskomponenten.config

import org.apache.kafka.clients.consumer.Consumer
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer

class ContainerStoppingFailedJsonAwareErrorHandler(
    private val valueDeserializer: DeserializerFailedJsonAware<*>
) :
    CommonContainerStoppingErrorHandler() {

    override fun handleOtherException(
        thrownException: java.lang.Exception,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        batchListener: Boolean
    ) {
        val failedJson = valueDeserializer.getFailedJson()

        if (failedJson != null) {
            println("Failed to deserialize JSON: $failedJson")
        }
        super.handleOtherException(thrownException, consumer, container, batchListener)
    }
}
