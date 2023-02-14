package no.nav.faktureringskomponenten.config

import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.RecordDeserializationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
class ContainerStoppingFailedJsonAwareErrorHandler(
    private val valueDeserializer: DeserializerJsonAware,
    @Value("\${kafka.consumer.oebs.topic}") private val topic: String
) : CommonContainerStoppingErrorHandler() {

    @Autowired
    private lateinit var fakturaMottakFeilRepository: FakturaMottakFeilRepository

    override fun handleRemaining(
        thrownException: Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer
    ) {
        saveError(thrownException, consumer)
        super.handleRemaining(thrownException, records, consumer, container)
    }

    override fun handleOtherException(
        thrownException: Exception,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        batchListener: Boolean
    ) {
        saveError(thrownException, consumer)
        super.handleOtherException(thrownException, consumer, container, batchListener)
    }

    private fun saveError(thrownException: Exception, consumer: Consumer<*, *>) {
        val getOffset: () -> Long = {
            val topicPartition = TopicPartition(topic, 0)
            consumer.position(topicPartition)
        }

        val json = valueDeserializer.getJson()
        val recordDeserializationException = thrownException as? RecordDeserializationException
        val offset = recordDeserializationException?.offset() ?: getOffset()
        fakturaMottakFeilRepository.saveAndFlush(
            FakturaMottakFeil(
                error = thrownException.cause?.message ?: thrownException.message,
                kafkaMelding = json,
                kafkaOffset = offset
            )
        )
    }
}
