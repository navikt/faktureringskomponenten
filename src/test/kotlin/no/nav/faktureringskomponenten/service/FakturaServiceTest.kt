package no.nav.faktureringskomponenten.service

import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifySequence
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltLinjeDto
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

class FakturaServiceTest {

    private val fakturaRepository = mockk<FakturaRepository>(relaxed = true)
    private val fakturaserieRepository = mockk<FakturaserieRepository>(relaxed = true)
    private val fakturaBestiltProducer = mockk<FakturaBestiltProducer>(relaxed = true)

    private val fakturaService = FakturaService(fakturaRepository, fakturaserieRepository, fakturaBestiltProducer)

    @Test
    fun `Bestiller bestillingsklare faktura og lagrer i databasen`() {
        val faktura = lagFaktura(1)

        every {
            fakturaRepository.findById(1)
        } returns faktura

        every {
            fakturaserieRepository.save(any())
        } returns faktura.getFakturaserie()!!

        every {
            fakturaRepository.save(any())
        } returns faktura

        every {
            fakturaserieRepository.findById(faktura.getFakturaserieId()!!)
        } returns Optional.of(faktura.getFakturaserie()!!)

        fakturaService.bestillFaktura(1)

        verifySequence {
            fakturaRepository.findById(1)
            fakturaserieRepository.findById(faktura.getFakturaserieId()!!)
            fakturaBestiltProducer.produserBestillingsmelding(any())
            fakturaserieRepository.save(faktura.getFakturaserie()!!)
            fakturaRepository.save(faktura)
        }
    }

    @Test
    fun `Bestiller bestillingsklare faktura med riktig data`() {
        val faktura = lagFaktura(1)
        val fakturaBestiltDtoCapturingSlot = slot<FakturaBestiltDto>()

        every {
            fakturaRepository.findById(1)
        } returns faktura

        every {
            fakturaserieRepository.save(any())
        } returns faktura.getFakturaserie()!!

        every {
            fakturaserieRepository.findById(faktura.getFakturaserieId()!!)
        } returns Optional.of(faktura.getFakturaserie()!!)

        every {
            fakturaRepository.save(any())
        } returns faktura

        every {
            fakturaBestiltProducer.produserBestillingsmelding(capture(fakturaBestiltDtoCapturingSlot))
        } answers {
            fakturaBestiltDtoCapturingSlot.captured.shouldBeEqualToComparingFields(
                FakturaBestiltDto(
                    fodselsnummer = BigDecimal(12345678911),
                    fullmektigOrgnr = "",
                    fullmektigFnr = BigDecimal(12129012345),
                    vedtaksId = "MEL-1",
                    fakturaReferanseNr = "",
                    kreditReferanseNr = "",
                    referanseBruker = "Referanse bruker",
                    referanseNAV = "Referanse NAV",
                    beskrivelse = "FTRL",
                    faktureringsDato = LocalDate.of(2022, 5, 1),
                    fakturaLinjer = listOf(
                        FakturaBestiltLinjeDto(
                            beskrivelse = "Periode: 01.01.2023 - 01.05.2023, En beskrivelse",
                            antall = 1.0,
                            enhetspris = BigDecimal(18000),
                            belop = BigDecimal(90000)
                        )
                    )
                )
            )
        }

        fakturaService.bestillFaktura(1)
    }

    fun lagFaktura(id: Long? = 1): Faktura {
        return Faktura(
            id,
            LocalDate.of(2022, 5, 1),
            FakturaStatus.OPPRETTET,
            fakturaLinje = listOf(
                FakturaLinje(
                    100,
                    LocalDate.of(2023, 1, 1),
                    LocalDate.of(2023, 5, 1),
                    beskrivelse = "En beskrivelse",
                    belop = BigDecimal(90000),
                    enhetsprisPerManed = BigDecimal(18000)
                ),
            )
        ).apply {
            setFakturaserie(
                Fakturaserie(
                    100, vedtaksId = "MEL-1",
                    fakturaGjelder = "FTRL",
                    referanseBruker = "Referanse bruker",
                    referanseNAV = "Referanse NAV",
                    startdato = LocalDate.of(2022, 1, 1),
                    sluttdato = LocalDate.of(2023, 5, 1),
                    status = FakturaserieStatus.OPPRETTET,
                    intervall = FakturaserieIntervall.KVARTAL,
                    faktura = listOf(),
                    fullmektig = Fullmektig(
                        fodselsnummer = BigDecimal(12129012345),
                        kontaktperson = "Test",
                        organisasjonsnummer = ""
                    ),
                    fodselsnummer = BigDecimal(12345678911)
                )
            )
        }
    }
}