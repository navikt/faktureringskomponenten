package no.nav.faktureringskomponenten.service.integration.kafka

import no.nav.faktureringskomponenten.PostgresTestContainerBase
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.forTest
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import org.springframework.context.annotation.Import
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional
import ulid.ULID

@EmbeddedKafka(
    count = 1, controlledShutdown = true, partitions = 1,
    topics = ["faktura-mottatt-topic-local", "faktura-bestilt.v1"],
    brokerProperties = ["offsets.topic.replication.factor=1", "transaction.state.log.replication.factor=1", "transaction.state.log.min.isr=1"]
)
@DirtiesContext
@Import(KafkaTestConfig::class)
open class EmbeddedKafkaBase(
    private val fakturaserieRepository: FakturaserieRepository,
) : PostgresTestContainerBase() {

    @Transactional
    protected open fun lagFakturaMedSerie(faktura: Faktura, referanse: String = ULID.randomULID()): Faktura {
        val serie = fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                this.referanse = referanse
                fodselsnummer = "01234567890"
                leggTilFaktura(faktura)
            }
        )
        val savedFaktura = serie.faktura.first()
        addCleanUpAction {
            fakturaserieRepository.findByReferanse(referanse)?.let {
                fakturaserieRepository.delete(it)
            }
        }
        return savedFaktura
    }

    companion object {
        const val kafkaTopic = "faktura-mottatt-topic-local"
    }
}
