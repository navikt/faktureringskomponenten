package no.nav.faktureringskomponenten

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DBVerify(
    @Autowired private val fakturaRepository: FakturaRepository,
    @Autowired private val fakturaserieRepository: FakturaserieRepository,
) {

    @Transactional
    fun databaseShouldBeClean() {
        withClue("fakturaRepository should be empty") {
            fakturaRepository.findAll().shouldBeEmpty()
        }
        withClue("fakturaserieRepository should be empty") {
            fakturaserieRepository.findAll().shouldBeEmpty()
        }
    }

}