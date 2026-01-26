package no.nav.faktureringskomponenten.controller.validators

object OrganisasjonsnummerValidator {
    fun erGyldig(organisasjonsnummer: String): Boolean {
        return organisasjonsnummer.matches(Regex("[0-9]+")) && organisasjonsnummer.length == 9
    }
}
