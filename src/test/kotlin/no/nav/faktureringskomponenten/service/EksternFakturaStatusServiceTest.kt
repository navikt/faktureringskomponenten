package no.nav.faktureringskomponenten.service

import io.mockk.every
import io.mockk.mockk
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.integration.kafka.ManglendeFakturabetalingProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import no.nav.faktureringskomponenten.service.mappers.EksternFakturaStatusMapper
import org.junit.jupiter.api.Test
import java.time.LocalDate

class EksternFakturaStatusServiceTest {

    private val fakturaRepository = mockk<FakturaRepository>()
    private val eksternFakturaStatusMapper = mockk<EksternFakturaStatusMapper>()
    private val manglendeFakturabetalingProducer = mockk<ManglendeFakturabetalingProducer>()
    private val service = EksternFakturaStatusService(fakturaRepository, eksternFakturaStatusMapper, manglendeFakturabetalingProducer)

    @Test
    fun `lagreEksternFakturaStatusMelding kaster ExternalErrorException når status er feil`() {
        every { fakturaRepository.findByReferanseNr("123") } returns null

        val eksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = "123",
            fakturaNummer = "82",
            dato = LocalDate.of(2023, 2, 1),
            status = FakturaStatus.MANGLENDE_INNBETALING,
            fakturaBelop = null,
            ubetaltBelop = null,
            feilmelding = "Feilmelding"
        )

        org.junit.jupiter.api.assertThrows<RessursIkkeFunnetException> {
            service.lagreEksternFakturaStatusMelding(eksternFakturaStatusDto)
        }
    }
}