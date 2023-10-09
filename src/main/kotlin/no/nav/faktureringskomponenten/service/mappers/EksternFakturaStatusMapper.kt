package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.domain.models.EksternFakturaStatus
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import org.springframework.stereotype.Component

@Component
class EksternFakturaStatusMapper {

    fun tilEksternFakturaStatus(
        eksternFakturaStatus: EksternFakturaStatusDto
    ): EksternFakturaStatus = EksternFakturaStatus(
            id = null,
            fakturaNummer = eksternFakturaStatus.fakturaNummer,
            dato = eksternFakturaStatus.dato,
            status = eksternFakturaStatus.status,
            fakturaBelop = eksternFakturaStatus.fakturaBelop,
            ubetaltBelop = eksternFakturaStatus.ubetaltBelop,
            feilMelding = eksternFakturaStatus.feilmelding
        )
}
