package no.nav.faktureringskomponenten.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.lagFaktura
import no.nav.faktureringskomponenten.lagFakturaserie
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

    private val fakturaBestillingService =
        FakturaBestillingService(fakturaRepository, fakturaserieRepository, fakturaBestiltProducer)

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
        val startDatoFaktura = faktura.fakturaLinje.minByOrNull { it.periodeFra }!!.periodeFra
        val sluttDatoFaktura = faktura.fakturaLinje.maxByOrNull { it.periodeFra }!!.periodeTil

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
                    beskrivelse = "Faktura Trygdeavgift ${startDatoFaktura.get(IsoFields.QUARTER_OF_YEAR)}-${
                        sluttDatoFaktura.get(
                            IsoFields.QUARTER_OF_YEAR
                        )
                    }. kvartal ${startDatoFaktura.year}",
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

    @Test
    fun `Kreditnota sendes til OEBS - serie og faktura får oppdatert status`() {
        val fakturaserie = lagFakturaserie {
            faktura(
                lagFaktura {
                    kreditReferanseNr(ULID.randomULID())
                },
                lagFaktura {
                    kreditReferanseNr(ULID.randomULID())
                }
            )
        }

        every { fakturaserieRepository.findByReferanse(fakturaserie.referanse) } returns fakturaserie
        every { fakturaserieRepository.save(any()) } returns mockk()


        fakturaBestillingService.bestillKreditnota(fakturaserie.referanse)


        fakturaserie.run {
            status.shouldBe(FakturaserieStatus.FERDIG)
            faktura.run {
                shouldHaveSize(2)
                forEach {
                    it.status.shouldBe(FakturaStatus.BESTILT)
                }
            }
        }

        verify { fakturaserieRepository.save(fakturaserie) }
        verify(exactly = 2) { fakturaBestiltProducer.produserBestillingsmelding(any()) }
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
                    beskrivelse = "En beskrivelse",
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
