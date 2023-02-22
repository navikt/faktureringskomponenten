package no.nav.faktureringskomponenten.service.integration.kafka

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
import org.junit.jupiter.api.Assertions.*
import org.springframework.context.annotation.Import
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext

@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["faktura-mottatt-topic-local"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@DirtiesContext
@Import(KafkaTestConfig::class)
open class EmbeddedKafkaBase(
    private val fakturaserieRepository: FakturaserieRepository,
) : PostgresTestContainerBase() {

    protected fun lagFakturaMedSerie(faktura: Faktura, vedtaksId: String = "MEL-1-1"): Faktura =
        fakturaserieRepository.saveAndFlush(
            Fakturaserie(
                vedtaksId = vedtaksId,
                fodselsnummer = "01234567890",
                faktura = mutableListOf(faktura)
            ).apply { this.faktura.forEach { it.fakturaserie = this } }
        ).faktura.first().apply {
            addCleanUpAction { fakturaserieRepository.delete(fakturaserie!!) }
        }

    companion object {
        const val kafkaTopic = "faktura-mottatt-topic-local"
    }
}
