package no.nav.faktureringskomponenten.controller

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.FakturaMottakFeil
import no.nav.faktureringskomponenten.domain.repositories.FakturaMottakFeilRepository
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Protected
@Validated
@RestController
@RequestMapping("/admin")
class AdminController(
    val fakturaMottakFeilRepository: FakturaMottakFeilRepository,
) {
    @GetMapping("/faktura/mottak/feil")
    fun hentFakturaMottakFeil(): ResponseEntity<Map<Long?, List<FakturaMottakFeil>>> {
        val groupBy: Map<Long?, List<FakturaMottakFeil>> =
            fakturaMottakFeilRepository.findAll().groupBy { it.kafkaOffset ?: -1 }
        return ResponseEntity.ok(groupBy.toSortedMap(compareBy { it }))
    }

}
