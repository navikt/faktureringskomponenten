package no.nav.faktureringskomponenten.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import no.nav.faktureringskomponenten.service.FakturaService
import org.springframework.stereotype.Component

/**
 * Henter antall feilede fakturaer fra databasen og legger til i en gauge som brukes i prometheus.
 */
@Component
class FakturaFeiletMetrikkerBinder(val fakturaService: FakturaService) : MeterBinder {

    override fun bindTo(meterRegistry: MeterRegistry) {
        meterRegistry.gauge(MetrikkNavn.FAKTURA_FEILET, fakturaService) { it.hentAntallFeiledeFakturaer().toDouble() }
    }
}