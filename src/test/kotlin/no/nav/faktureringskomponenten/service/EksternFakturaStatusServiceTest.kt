package no.nav.faktureringskomponenten.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.faktureringskomponenten.domain.models.EksternFakturaStatus
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.forTest
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.integration.kafka.ManglendeFakturabetalingProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.Betalingsstatus
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.ManglendeFakturabetalingDto
import no.nav.faktureringskomponenten.service.mappers.EksternFakturaStatusMapper
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class EksternFakturaStatusServiceTest {

    private val fakturaRepository = mockk<FakturaRepository>()
    private val eksternFakturaStatusMapper = mockk<EksternFakturaStatusMapper>()
    private val manglendeFakturabetalingProducer = mockk<ManglendeFakturabetalingProducer>(relaxed = true)
    private val service =
        EksternFakturaStatusService(fakturaRepository, eksternFakturaStatusMapper, manglendeFakturabetalingProducer)

    @Test
    fun `håndterEksternFakturaStatusMelding kaster ExternalErrorException når status er feil`() {
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


        shouldThrow<RessursIkkeFunnetException> {
            service.håndterEksternFakturaStatusMelding(eksternFakturaStatusDto)
        }.run {
            message shouldBe "Finner ikke faktura med faktura referanse nr 123"
            field shouldBe "faktura.referanseNr"
        }
    }

    @Test
    fun `håndterEksternFakturaStatusMelding sender ikke kafkamelding når melding fra OEBS er duplikat`() {
        val eksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = "123",
            fakturaNummer = "82",
            dato = LocalDate.of(2023, 2, 1),
            status = FakturaStatus.MANGLENDE_INNBETALING,
            fakturaBelop = BigDecimal(1000),
            ubetaltBelop = BigDecimal(1000),
            feilmelding = null
        )
        val eksternFakturaStatus = EksternFakturaStatus(
            id = 1L,
            status = eksternFakturaStatusDto.status,
            feilMelding = eksternFakturaStatusDto.feilmelding,
        )
        val faktura = Faktura.forTest {
            id = 1L
            status = FakturaStatus.MANGLENDE_INNBETALING
            this.eksternFakturaStatus = mutableListOf(eksternFakturaStatus)
        }

        every { fakturaRepository.findByReferanseNr("123") } returns faktura
        every {
            eksternFakturaStatusMapper.tilEksternFakturaStatus(
                eksternFakturaStatusDto,
                any()
            )
        } returns eksternFakturaStatus
        every { fakturaRepository.save(any()) } returns faktura


        service.håndterEksternFakturaStatusMelding(eksternFakturaStatusDto)


        verify { manglendeFakturabetalingProducer wasNot Called }
        verify { fakturaRepository.save(any()) }
    }

    @Test
    fun `håndterEksternFakturaStatusMelding sender kafkamelding ved manglende betaling med status IKKE_BETALT`() {
        val eksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = "123",
            fakturaNummer = "82",
            dato = LocalDate.of(2023, 2, 1),
            status = FakturaStatus.MANGLENDE_INNBETALING,
            fakturaBelop = BigDecimal(1000),
            ubetaltBelop = BigDecimal(1000),
            feilmelding = null
        )
        val faktura = Faktura.forTest {
            id = 1L
            referanseNr = "123"
            fakturaserie = Fakturaserie.forTest {
                referanse = "321"
            }
        }
        every { fakturaRepository.findByReferanseNr("123") } returns faktura
        every {
            eksternFakturaStatusMapper.tilEksternFakturaStatus(
                eksternFakturaStatusDto,
                any()
            )
        } returns EksternFakturaStatus(
            id = null,
            dato = eksternFakturaStatusDto.dato,
            status = eksternFakturaStatusDto.status,
            fakturaBelop = eksternFakturaStatusDto.fakturaBelop,
            ubetaltBelop = eksternFakturaStatusDto.ubetaltBelop,
            feilMelding = eksternFakturaStatusDto.feilmelding,
            faktura = faktura
        )
        every { fakturaRepository.save(any()) } returns faktura
        val manglendeFakturabetalingSlot = slot<ManglendeFakturabetalingDto>()


        service.håndterEksternFakturaStatusMelding(eksternFakturaStatusDto)


        verify { manglendeFakturabetalingProducer.produserBestillingsmelding(capture(manglendeFakturabetalingSlot)) }
        manglendeFakturabetalingSlot.captured.run {
            fakturaserieReferanse shouldBe "321"
            betalingsstatus shouldBe Betalingsstatus.IKKE_BETALT
            fakturanummer shouldBe "82"
            datoMottatt shouldBe LocalDate.of(2023, 2, 1)
        }
    }

    @Test
    fun `håndterEksternFakturaStatusMelding sender kafkamelding ved manglende betaling med status DELVIS_BETALT`() {
        val eksternFakturaStatusDto = EksternFakturaStatusDto(
            fakturaReferanseNr = "123",
            fakturaNummer = "82",
            dato = LocalDate.of(2023, 2, 1),
            status = FakturaStatus.MANGLENDE_INNBETALING,
            fakturaBelop = BigDecimal(2000),
            ubetaltBelop = BigDecimal(1000),
            feilmelding = null
        )
        val faktura = Faktura.forTest {
            id = 1L
            referanseNr = "123"
            fakturaserie = Fakturaserie.forTest {
                referanse = "321"
            }
        }
        every { fakturaRepository.findByReferanseNr("123") } returns faktura
        every {
            eksternFakturaStatusMapper.tilEksternFakturaStatus(
                eksternFakturaStatusDto,
                any()
            )
        } returns EksternFakturaStatus(
            id = null,
            dato = eksternFakturaStatusDto.dato,
            status = eksternFakturaStatusDto.status,
            fakturaBelop = eksternFakturaStatusDto.fakturaBelop,
            ubetaltBelop = eksternFakturaStatusDto.ubetaltBelop,
            feilMelding = eksternFakturaStatusDto.feilmelding,
            faktura = faktura
        )
        every { fakturaRepository.save(any()) } returns faktura
        val manglendeFakturabetalingSlot = slot<ManglendeFakturabetalingDto>()


        service.håndterEksternFakturaStatusMelding(eksternFakturaStatusDto)


        verify { manglendeFakturabetalingProducer.produserBestillingsmelding(capture(manglendeFakturabetalingSlot)) }
        manglendeFakturabetalingSlot.captured.run {
            fakturaserieReferanse shouldBe "321"
            betalingsstatus shouldBe Betalingsstatus.DELVIS_BETALT
            fakturanummer shouldBe "82"
            datoMottatt shouldBe LocalDate.of(2023, 2, 1)
        }
    }
}
