package no.nav.faktureringskomponenten.service.integration.kafka.dto

import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import java.math.BigDecimal

data class FakturaMottattDto(

    val fodselsnummer: String,
    val vedtaksId: String?,
    val fakturaReferanseNr: String,
    val kreditReferanseNr: String?,
    val belop: BigDecimal,
    val status: FakturaStatus,
)
