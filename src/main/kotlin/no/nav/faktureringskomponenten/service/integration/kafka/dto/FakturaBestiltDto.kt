package no.nav.faktureringskomponenten.service.integration.kafka.dto

import java.math.BigDecimal
import java.time.LocalDate

data class FakturaBestiltDto(

    val fodselsnummer: BigDecimal,
    val fullmektigOrgnr: String?,
    val fullmektigFnr: BigDecimal?,
    val vedtaksId: String,
    val fakturaReferanseNr: String,
    val kreditReferanseNr: String?,
    val referanseBruker: String,
    val referanseNAV: String,
    val beskrivelse: String,
    val fakturaLinjer: List<FakturaBestiltLinjeDto>,
    val faktureringsDato: LocalDate
)