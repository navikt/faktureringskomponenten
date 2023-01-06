package no.nav.faktureringskomponenten.config.metrics

class MetricsNavn {
    companion object {
        val METRIKKER_NAMESPACE = "faktureringskomponenten"

        val FAKTURASERIE_OPPRETTET = "${METRIKKER_NAMESPACE}.fakturaserie.opprettet.antall"
        val FAKTURA_SENDT = "${METRIKKER_NAMESPACE}.faktura.sendt.antall"
    }
}