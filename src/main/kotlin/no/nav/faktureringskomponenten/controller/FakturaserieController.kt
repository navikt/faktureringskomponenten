package no.nav.faktureringskomponenten.controller

import io.getunleash.Unleash
import io.micrometer.core.instrument.Metrics
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import mu.KotlinLogging
import no.nav.faktureringskomponenten.config.ToggleName
import no.nav.faktureringskomponenten.controller.dto.FakturamottakerRequestDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieRequestDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.dto.NyFakturaserieResponseDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturamottakerDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturaserieDto
import no.nav.faktureringskomponenten.controller.mapper.tilFakturaserieResponseDto
import no.nav.faktureringskomponenten.exceptions.ProblemDetailFactory
import no.nav.faktureringskomponenten.exceptions.ProblemDetailFactory.Companion.mapTilProblemDetail
import no.nav.faktureringskomponenten.metrics.MetrikkNavn
import no.nav.faktureringskomponenten.service.FakturaserieService
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*


private val log = KotlinLogging.logger { }

@Protected
@Validated
@RestController
@RequestMapping("/fakturaserier")
class FakturaserieController @Autowired constructor(
    val faktureringService: FakturaserieService,
    val unleash: Unleash
) {

    @Operation(summary = "Lager en ny fakturaserie")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Fakturaserie opprettet"),
            ApiResponse(responseCode = "400", description = "Feil med validering av felter")
        ]
    )

    @ProtectedWithClaims(issuer = "aad", claimMap = ["roles=faktureringskomponenten-skriv"])
    @PostMapping
    fun lagNyFakturaserie(
        @RequestBody @Validated fakturaserieRequestDto: FakturaserieRequestDto,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        log.info("Mottatt $fakturaserieRequestDto")

        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ProblemDetailFactory.of(bindingResult))
        }

        if (unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
            if (fakturaserieRequestDto.perioder.isEmpty() && fakturaserieRequestDto.fakturaserieReferanse.isNullOrEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapTilProblemDetail("perioder", "Må ha minst en periode hvis ikke er erstatning av tidligere fakturaserie"))
            }
        } else if (fakturaserieRequestDto.perioder.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapTilProblemDetail("perioder", "Du må oppgi minst én periode"))
        }

        val forrigeReferanse = fakturaserieRequestDto.fakturaserieReferanse
        val fakturaserieDto = fakturaserieRequestDto.tilFakturaserieDto
        val referanse = faktureringService.lagNyFakturaserie(fakturaserieDto, forrigeReferanse)
        Metrics.counter(MetrikkNavn.FAKTURASERIE_OPPRETTET).increment()

        return ResponseEntity.ok(NyFakturaserieResponseDto(referanse))
    }

    @ProtectedWithClaims(issuer = "aad", claimMap = ["roles=faktureringskomponenten-skriv"])
    @PutMapping("/{referanse}/mottaker")
    fun oppdaterFakturaMottaker(
        @RequestBody @Validated fakturamottakerRequestDto: FakturamottakerRequestDto,
        @PathVariable("referanse", required = true) referanse: String,
        bindingResult: BindingResult
    ): ResponseEntity<Any> {
        log.info("Mottatt forespørsel om endring av fakturamottaker for ${referanse}")

        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ProblemDetailFactory.of(bindingResult))
        }

        val fakturamottakerDto = fakturamottakerRequestDto.tilFakturamottakerDto
        faktureringService.endreFakturaMottaker(referanse, fakturamottakerDto)


        return ResponseEntity.ok().build()
    }

    @Operation(summary = "Henter fakturaserie på referanse")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "400", description = "Fant ikke forespurt fakturaserie")
        ]
    )
    @GetMapping("/{referanse}")
    fun hentFakturaserie(@PathVariable("referanse") referanse: String): FakturaserieResponseDto {
        return faktureringService.hentFakturaserie(referanse).tilFakturaserieResponseDto()
    }

    @GetMapping
    fun hentFakturaserier(
        @RequestParam("referanse") referanse: String,
    ): List<FakturaserieResponseDto> {
        return faktureringService.hentFakturaserier(referanse).map { it.tilFakturaserieResponseDto() }
    }

    @ProtectedWithClaims(issuer = "aad", claimMap = ["roles=faktureringskomponenten-skriv"])
    @DeleteMapping("/{referanse}")
    fun kansellerFakturaserie(
        @PathVariable("referanse", required = true) referanse: String,
    ): ResponseEntity<NyFakturaserieResponseDto> {
        log.info("Mottatt forespørsel om kansellering av fakturaserie: ${referanse}")

        val nyFakturaserieRefereanse = faktureringService.kansellerFakturaserie(referanse)

        log.info("Kansellert fakturaserie med referanse ${referanse}, Ny fakturaseriereferanse: ${nyFakturaserieRefereanse}")
        return ResponseEntity.ok(NyFakturaserieResponseDto(nyFakturaserieRefereanse))
    }
}
