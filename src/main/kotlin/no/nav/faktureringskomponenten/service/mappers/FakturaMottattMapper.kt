package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.domain.models.FakturaMottatt
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import org.springframework.stereotype.Component

@Component
class FakturaMottattMapper {

    fun tilFakturaMottat(
        fakturaMottatt: FakturaMottattDto
    ): FakturaMottatt = FakturaMottatt(
            id = null,
            fakturaReferanseNr = fakturaMottatt.fakturaReferanseNr,
            fakturaNummer = fakturaMottatt.fakturanummer,
            dato = fakturaMottatt.dato,
            status = fakturaMottatt.status,
            fakturaBelop = fakturaMottatt.fakturaBeløp,
            ubetaltBelop = fakturaMottatt.ubetaltBeløp,
            feilMelding = fakturaMottatt.feilmelding
        )
}
