package no.nav.faktureringskomponenten.service.integration.kafka.dto

import java.math.BigDecimal

data class FakturaBestiltLinjeDto(
    val beskrivelse: String,
    val antall: Double,
    val enhetspris: BigDecimal,
    val belop: BigDecimal,
)