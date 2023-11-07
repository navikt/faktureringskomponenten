package no.nav.faktureringskomponenten.service

import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.exceptions.EksternFeilException
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.integration.kafka.ManglendeFakturabetalingProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import no.nav.faktureringskomponenten.service.mappers.EksternFakturaStatusMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate

class EksternFakturaStatusServiceTest {

    private val fakturaRepository = mock<FakturaRepository>()
    private val eksternFakturaStatusMapper = mock<EksternFakturaStatusMapper>()
    private val manglendeFakturabetalingProducer = mock<ManglendeFakturabetalingProducer>()
    private val service = EksternFakturaStatusService(fakturaRepository, eksternFakturaStatusMapper, manglendeFakturabetalingProducer)

    @Test
    fun `lagreEksternFakturaStatusMelding kaster ExternalErrorException når status er FEIL`() {

        val eksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = "123",
            fakturaNummer = "82",
            dato = LocalDate.of(2023, 2, 1),
            status = FakturaStatus.FEIL,
            fakturaBelop = null,
            ubetaltBelop = null,
            feilmelding = "Feilmelding"
        )

        val exception = org.junit.jupiter.api.assertThrows<EksternFeilException> {
            service.lagreEksternFakturaStatusMelding(eksternFakturaStatusDto)
        }

        exception.message shouldBe "EksternFakturaStatus er FEIL. Feilmelding fra OEBS: Feilmelding"
    }

    @Test
    fun `lagreEksternFakturaStatusMelding kaster ExternalErrorException når status er FEIL2`() {
        `when`(fakturaRepository.findByReferanseNr("123")).thenReturn(null)

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