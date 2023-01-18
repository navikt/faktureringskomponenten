package no.nav.faktureringskomponenten.domain.repositories

import io.kotest.matchers.collections.shouldHaveSize
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@ActiveProfiles("itest")
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FakturaserieRepositoryIT(
    @Autowired val fakturaRepository: FakturaRepository,
    @Autowired val fakturaserieRepository: FakturaserieRepository,
) : PostgresTestContainerBase() {

    @Test
    fun test_findAllByDatoBestiltIsLessThanEqualAndStatusIs() {
        fakturaserieRepository.save(
            Fakturaserie(
                faktura = listOf(
                    Faktura(datoBestilt = LocalDate.now().plusDays(-1))
                )
            )
        )

        val fakturaList =
            fakturaRepository.findAllByDatoBestiltIsLessThanEqualAndStatusIsOpprettet(LocalDate.now())

        fakturaList.forEach { println(it) }

        fakturaList.shouldHaveSize(1)
    }
}
