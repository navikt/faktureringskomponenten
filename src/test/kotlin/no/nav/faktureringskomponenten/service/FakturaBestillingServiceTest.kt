package no.nav.faktureringskomponenten.service

import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.mockk.*
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltLinjeDto
import org.junit.jupiter.api.Test
import ulid.ULID
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.IsoFields

class FakturaBestillingServiceTest {

    private val fakturaRepository = mockk<FakturaRepository>(relaxed = true)
    private val fakturaserieRepository = mockk<FakturaserieRepository>(relaxed = true)
    private val fakturaBestiltProducer = mockk<FakturaBestiltProducer>(relaxed = true)

    private val fakturaBestillingService = FakturaBestillingService(fakturaRepository, fakturaserieRepository, fakturaBestiltProducer)

    @Test
    fun `Bestiller bestillingsklare faktura og lagrer i databasen`() {
        val fakturaReferanseNr = ULID.randomULID()
        val faktura = lagFaktura(fakturaReferanseNr)

        every {
            fakturaRepository.findByReferanseNr(fakturaReferanseNr)
        } returns faktura

        every {
            fakturaserieRepository.save(any())
        } returns faktura.fakturaserie!!

        every {
            fakturaRepository.save(any())
        } returns faktura

        every {
            fakturaserieRepository.findById(faktura.getFakturaserieId()!!)
        } returns faktura.fakturaserie!!

        fakturaBestillingService.bestillFaktura(fakturaReferanseNr)

        verifySequence {
            fakturaRepository.findByReferanseNr(fakturaReferanseNr)
            fakturaserieRepository.findById(faktura.getFakturaserieId()!!)
            fakturaserieRepository.save(faktura.fakturaserie!!)
            fakturaRepository.save(faktura)
            fakturaBestiltProducer.produserBestillingsmelding(any())
        }
    }

    @Test
    fun `Bestiller bestillingsklare faktura med riktig data`() {
        val fakturaReferanseNr = ULID.randomULID()
        val faktura = lagFaktura(fakturaReferanseNr)
        val fakturaBestiltDtoCapturingSlot = slot<FakturaBestiltDto>()
        val nå = LocalDate.now()

        every {
            fakturaRepository.findByReferanseNr(fakturaReferanseNr)
        } returns faktura

        every {
            fakturaserieRepository.save(any())
        } returns faktura.fakturaserie!!

        every {
            fakturaserieRepository.findById(faktura.getFakturaserieId()!!)
        } returns faktura.fakturaserie!!

        every {
            fakturaRepository.save(any())
        } returns faktura

        every {
            fakturaBestiltProducer.produserBestillingsmelding(capture(fakturaBestiltDtoCapturingSlot))
        } answers {
            fakturaBestiltDtoCapturingSlot.captured.shouldBeEqualToComparingFields(
                FakturaBestiltDto(
                    fodselsnummer = "12345678911",
                    fullmektigOrgnr = "",
                    fullmektigFnr = "12129012345",
                    fakturaserieReferanse = "MEL-1",
                    fakturaReferanseNr = fakturaReferanseNr,
                    kreditReferanseNr = "",
                    referanseBruker = "Referanse bruker",
                    referanseNAV = "Referanse NAV",
                    beskrivelse = "Faktura Trygdeavgift ${nå.get(IsoFields.QUARTER_OF_YEAR)}. kvartal ${nå.year}",
                    artikkel = "F00008",
                    faktureringsDato = LocalDate.of(2022, 5, 1),
                    fakturaLinjer = listOf(
                        FakturaBestiltLinjeDto(
                            beskrivelse = "En beskrivelse",
                            antall = BigDecimal(1),
                            enhetspris = BigDecimal(18000),
                            belop = BigDecimal(90000)
                        )
                    )
                )
            )
        }

        fakturaBestillingService.bestillFaktura(fakturaReferanseNr)
    }

    private fun lagFaktura(fakturaReferanseNr: String? = ULID.randomULID()): Faktura {
        return Faktura(
            id = null,
            referanseNr = fakturaReferanseNr!!,
            LocalDate.of(2022, 5, 1),
            LocalDate.of(2022, 5, 1),
            FakturaStatus.OPPRETTET,
            fakturaLinje = listOf(
                FakturaLinje(
                    id = 100,
                    referertFakturaVedAvregning = null,
                    periodeFra = LocalDate.of(2023, 1, 1),
                    periodeTil = LocalDate.of(2023, 5, 1),
                    beskrivelse = "Periode: 01.01.2023 - 01.05.2023, En beskrivelse",
                    belop = BigDecimal(90000),
                    antall = BigDecimal(1),
                    enhetsprisPerManed = BigDecimal(18000)
                ),
            )
        ).apply {
            fakturaserie =
                Fakturaserie(
                    100, referanse = "MEL-1",
                    fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
                    referanseBruker = "Referanse bruker",
                    referanseNAV = "Referanse NAV",
                    startdato = LocalDate.of(2022, 1, 1),
                    sluttdato = LocalDate.of(2023, 5, 1),
                    status = FakturaserieStatus.OPPRETTET,
                    intervall = FakturaserieIntervall.KVARTAL,
                    faktura = mutableListOf(),
                    fullmektig = Fullmektig(
                        fodselsnummer = "12129012345",
                        organisasjonsnummer = ""
                    ),
                    fodselsnummer = "12345678911"
                )
        }
    }
}
