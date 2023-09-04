package no.nav.faktureringskomponenten.service.integration.kafka.dto

import java.time.LocalDate

data class FakturaBestiltDto(

    val fodselsnummer: String,
    val fullmektigOrgnr: String?,
    val fullmektigFnr: String?,
    val referanseId: String,
    val fakturaReferanseNr: String,
    val kreditReferanseNr: String?,
    val referanseBruker: String,
    val referanseNAV: String,
    val beskrivelse: String,
    val artikkel: String,
    val fakturaLinjer: List<FakturaBestiltLinjeDto>,
    val faktureringsDato: LocalDate
)