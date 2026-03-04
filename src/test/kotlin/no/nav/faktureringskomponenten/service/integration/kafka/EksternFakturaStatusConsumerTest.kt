package no.nav.faktureringskomponenten.service.integration.kafka

import io.mockk.mockk
import io.mockk.verify
import no.nav.faktureringskomponenten.service.EksternFakturaStatusService
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.listener.ConsumerSeekAware

class EksternFakturaStatusConsumerTest {

    private val eksternFakturaStatusService = mockk<EksternFakturaStatusService>(relaxed = true)
    private val kafkaListenerEndpointRegistry = mockk<KafkaListenerEndpointRegistry>(relaxed = true)

    @Test
    fun `settSpesifiktOffsetPåConsumer kaller seek på alle partitions med riktig offset`() {
        val callback = mockk<ConsumerSeekAware.ConsumerSeekCallback>(relaxed = true)
        val tp1 = TopicPartition("topic", 0)
        val tp2 = TopicPartition("topic", 1)
        val consumer = EksternFakturaStatusConsumer(eksternFakturaStatusService, kafkaListenerEndpointRegistry)
        consumer.registerSeekCallback(callback)
        consumer.onPartitionsAssigned(mapOf(tp1 to 0L, tp2 to 0L), callback)

        consumer.settSpesifiktOffsetPåConsumer(42L)

        verify { callback.seek("topic", 0, 42L) }
        verify { callback.seek("topic", 1, 42L) }
    }

    @Test
    fun `settSpesifiktOffsetPåConsumer med offset 0 søker til starten`() {
        val callback = mockk<ConsumerSeekAware.ConsumerSeekCallback>(relaxed = true)
        val tp = TopicPartition("topic", 0)
        val consumer = EksternFakturaStatusConsumer(eksternFakturaStatusService, kafkaListenerEndpointRegistry)
        consumer.registerSeekCallback(callback)
        consumer.onPartitionsAssigned(mapOf(tp to 0L), callback)

        consumer.settSpesifiktOffsetPåConsumer(0L)

        verify { callback.seek("topic", 0, 0L) }
    }
}
