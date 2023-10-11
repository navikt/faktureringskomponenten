package no.nav.faktureringskomponenten.controller

import io.micrometer.core.instrument.Metrics
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import no.nav.faktureringskomponenten.controller.dto.FakturaserieRequestDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.dto.NyFakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturaserieDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturaserieResponseDto
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.exceptions.ProblemDetailFactory
import no.nav.faktureringskomponenten.metrics.MetrikkNavn
import no.nav.faktureringskomponenten.service.EksternFakturaStatusService
import no.nav.faktureringskomponenten.service.FakturaserieService
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@Unprotected
@RestController
@RequestMapping("/eksternFakturaStatus")
class EksternFakturaStatusLiksomController @Autowired constructor(
    val eksternFakturaStatusService: EksternFakturaStatusService
) {

    @PostMapping
    fun lagNyMelding(
        @RequestBody eksternFakturaStatusDto: EksternFakturaStatusDto,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        eksternFakturaStatusService.lagreEksternFakturaStatusMelding(eksternFakturaStatusDto)
        return ResponseEntity.ok(true)
    }
}