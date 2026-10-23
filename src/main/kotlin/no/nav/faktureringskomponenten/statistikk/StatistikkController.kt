package no.nav.faktureringskomponenten.statistikk

import io.swagger.v3.oas.annotations.Operation
import no.nav.security.token.support.core.api.Protected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@RequestMapping("/admin/statistikk")
class StatistikkController(private val statistikkService: StatistikkService) {

    @Operation(
        summary = "Øyeblikksbilde av kvartalskjøringene og avvik i flyten mot OEBS",
        description = "Bare lesende og uten personopplysninger. Regnes ut på nytt ved hvert kall."
    )
    @GetMapping("/oyeblikksbilde")
    fun hentStatistikkOyeblikksbilde(): StatistikkOyeblikksbildeDto = statistikkService.hentOyeblikksbilde()
}
