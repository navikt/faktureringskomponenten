package no.nav.faktureringskomponenten.service.integration.kafka.dto

import java.math.BigDecimal

data class FakturaBestiltLinjeDto(
    val beskrivelse: String,
    val antall: BigDecimal,
    val enhetspris: BigDecimal,
    val belop: BigDecimal,
)