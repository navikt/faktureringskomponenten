package no.nav.faktureringskomponenten.controller.dto

import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import java.math.BigDecimal
import java.time.LocalDate

data class FakturaTilbakemeldingResponseDto(
    val dato: LocalDate?,
    val status: FakturaStatus?,
    val fakturaBelop: BigDecimal?,
    val ubetaltBelop: BigDecimal?,
    val feilmelding: String?,
)
