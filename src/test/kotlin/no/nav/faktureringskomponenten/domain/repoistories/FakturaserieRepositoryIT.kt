package no.nav.faktureringskomponenten.domain.repoistories

import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.testutils.PostgresTestContainerBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDate

@Testcontainers
@ActiveProfiles("itest")
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FakturaserieRepositoryIT(
    @Autowired val fakturaRepository: FakturaRepository,
    @Autowired val fakturaserieRepository: FakturaserieRepository,
) : PostgresTestContainerBase() {

    @Test
    fun test_findAllByDatoBestiltIsLessThanEqualAndStatusIs() {
        fakturaRepository.findAllByDatoBestiltIsLessThanEqualAndStatusIs(LocalDate.now(), FakturaStatus.OPPRETTET)


    }
}
