package no.nav.faktureringskomponenten.metrics

class MetrikkNavn {
    companion object {
        val METRIKKER_NAMESPACE = "faktureringskomponenten"

        val FAKTURASERIE_OPPRETTET = "${METRIKKER_NAMESPACE}.fakturaserie.opprettet"
        val FAKTURA_BESTILT = "${METRIKKER_NAMESPACE}.faktura.bestilt"

        // Blir lagt til når vi får statuser fra OEBS
        val FAKTURA_IKKE_BETALT_STATUS = "${METRIKKER_NAMESPACE}.faktura.ikkebetalt"

        // Blir lagt til når vi får flere enn én konsument
        val FAKTURASERIER_SYSTEM = "${METRIKKER_NAMESPACE}.fakturaserier.system"
    }
}
