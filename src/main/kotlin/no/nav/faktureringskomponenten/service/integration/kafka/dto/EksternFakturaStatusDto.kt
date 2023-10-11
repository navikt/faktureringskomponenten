package no.nav.faktureringskomponenten.service.integration.kafka.dto

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import java.math.BigDecimal
import java.time.LocalDate

data class EksternFakturaStatusDto(
    val fakturaReferanseNr: String,
    val fakturaNummer: String?,
    @JsonFormat(pattern = "dd-MM-yyyy")
    val dato: LocalDate,
    val status: FakturaStatus,
    val fakturaBelop: BigDecimal?,
    val ubetaltBelop: BigDecimal?,
    val feilmelding: String?,
)
