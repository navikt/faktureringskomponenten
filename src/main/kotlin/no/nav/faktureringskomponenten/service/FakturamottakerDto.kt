package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Fullmektig

data class FakturamottakerDto(
    val fakturaserieReferanse: String,
    val fullmektig: Fullmektig?,
)
