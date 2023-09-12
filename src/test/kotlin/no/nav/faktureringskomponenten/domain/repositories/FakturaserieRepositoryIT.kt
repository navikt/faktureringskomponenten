package no.nav.faktureringskomponenten.domain.repositories

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@ActiveProfiles("itest")
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FakturaserieRepositoryIT(
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
) : PostgresTestContainerBase() {

    @Test
    @Transactional
    fun test_findAllByDatoBestiltIsLessThanEqualAndStatusIs() {
        fakturaserieRepository.save(
            Fakturaserie(
                faktura = listOf(
                    Faktura(datoBestilt = LocalDate.now().plusDays(100))
                )
            )
        ).apply { addCleanUpAction { fakturaserieRepository.delete(this) } }

        val fakturaList =
            fakturaRepository.findAllByDatoBestiltIsLessThanEqualAndStatusIsOpprettet(LocalDate.now().plusDays(100))

        fakturaList.shouldNotBeEmpty()
    }

    @Test
    fun `lag fødselsnummer med 11 char og last igjen`() {
        val fakturaserie = fakturaserieRepository.save(
            Fakturaserie(
                fodselsnummer = "01234567890",
                fullmektig = Fullmektig(
                    fodselsnummer = "-123456789-"
                ),
                faktura = listOf()
            )
        ).apply { addCleanUpAction { fakturaserieRepository.delete(this) } }

        fakturaserieRepository.findById(
            fakturaserie.id.shouldNotBeNull()
        ).shouldNotBeNull()
            .apply {
                fodselsnummer.shouldBe("01234567890")
                fullmektig.shouldNotBeNull()
                    .fodselsnummer.shouldBe("-123456789-")
            }
    }
}
