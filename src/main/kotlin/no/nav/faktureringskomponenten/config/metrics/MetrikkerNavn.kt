package no.nav.faktureringskomponenten.config.metrics

class MetrikkerNavn {
    companion object {
        val METRIKKER_NAMESPACE = "faktureringskomponenten"

        val FAKTURASERIER_OPPRETTET = "${METRIKKER_NAMESPACE}.fakturaserier.opprettet"
        val FAKTURASERIER_SYSTEM = "${METRIKKER_NAMESPACE}.fakturaserier.system"
        val FAKTURA_SENDT = "${METRIKKER_NAMESPACE}.faktura.sendt"
        val FAKTURA_IKKE_BETALT_STATUS = "${METRIKKER_NAMESPACE}.faktura.ikkebetalt"
    }
}