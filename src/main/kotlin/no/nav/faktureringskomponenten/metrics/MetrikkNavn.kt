package no.nav.faktureringskomponenten.metrics

class MetrikkNavn {
    companion object {
        val METRIKKER_NAMESPACE = "faktureringskomponenten"

        val FAKTURASERIE_OPPRETTET = "${METRIKKER_NAMESPACE}.fakturaserie.opprettet"
        val FAKTURA_BESTILT = "${METRIKKER_NAMESPACE}.faktura.bestilt"
        val FAKTURA_FEILET_COUNTER = "${METRIKKER_NAMESPACE}.faktura.feilet.counter"
        val FAKTURA_FEILET = "${METRIKKER_NAMESPACE}.faktura.feilet"

        val FAKTURA_IKKE_BETALT_STATUS = "${METRIKKER_NAMESPACE}.faktura.ikkebetalt"

        val FAKTURASERIE_SYSTEM = "${METRIKKER_NAMESPACE}.fakturaserier.system"
    }
}
