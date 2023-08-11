package no.nav.faktureringskomponenten.service.integration.kafka.dto

import no.nav.faktureringskomponenten.domain.models.FakturaMottattStatus
import java.math.BigDecimal
import java.time.LocalDate

data class FakturaMottattDto(
    val fakturaReferanseNr: String,
    val fakturanummer: String?,
    val dato: LocalDate,
    val status: FakturaMottattStatus,
    val fakturaBeløp: BigDecimal?,
    val ubetaltBeløp: BigDecimal?,
    val feilmelding: String?,
)
