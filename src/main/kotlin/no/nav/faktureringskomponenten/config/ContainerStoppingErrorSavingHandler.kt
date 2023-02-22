package no.nav.faktureringskomponenten.config

import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaMottattConsumerException
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.RecordDeserializationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component

@Component
class ContainerStoppingErrorSavingHandler(
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
        val recordDeserializationException = thrownException as? RecordDeserializationException
        val fakturaMottattConsumerException = thrownException.cause as? FakturaMottattConsumerException
        val offset = recordDeserializationException?.offset() ?: fakturaMottattConsumerException?.offset
        if (offset == null) log.warn("Fant ikke kafka offset fra Exceptions", thrownException)
        fakturaMottakFeilRepository.saveAndFlush(
            FakturaMottakFeil(
                error = getErrorStack(thrownException), //thrownException.cause?.message ?: thrownException.message,
                kafkaMelding = valueDeserializer.json,
                kafkaOffset = offset
            )
        )
    }

    fun getErrorStack(throwable: Throwable?, message: String? = ""): String? {
        if (throwable?.message != null) return getErrorStack(
            throwable.cause,
            "${throwable.message} - (${throwable.javaClass.simpleName})\n$message"
        )
        return message
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ContainerStoppingErrorSavingHandler::class.java)
    }
}