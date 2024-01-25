package no.nav.faktureringskomponenten.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.lagFaktura
import no.nav.faktureringskomponenten.lagFakturalinje
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
        val faktura = lagFaktura {
            fakturaserie(
                lagFakturaserie {
                    id(10)
                }
            )
        }

        every {
            fakturaRepository.findByReferanseNr(faktura.referanseNr)
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

        fakturaBestillingService.bestillFaktura(faktura.referanseNr)

        verifySequence {
            fakturaRepository.findByReferanseNr(faktura.referanseNr)
            fakturaserieRepository.findById(faktura.getFakturaserieId()!!)
            fakturaserieRepository.save(faktura.fakturaserie!!)
            fakturaRepository.save(faktura)
            fakturaBestiltProducer.produserBestillingsmelding(any())
        }
    }

    @Test
    fun `Bestiller bestillingsklare faktura med riktig data`() {
        val faktura = lagFaktura {
            datoBestilt(LocalDate.of(2022, 5, 1))
            fakturaLinje(
                lagFakturalinje {
                    antall(BigDecimal.ONE)
                    enhetsprisPerManed(BigDecimal(18000))
                    belop(BigDecimal(90000))
                    periodeFra(LocalDate.of(2022, 1, 1))
                    periodeTil(LocalDate.of(2022, 5, 1))
                    beskrivelse("En beskrivelse")
                }
            )
            fakturaserie(
                lagFakturaserie {
                    id(10)
                    fullmektig(Fullmektig(fodselsnummer = "12129012345", organisasjonsnummer = ""))
                    startdato(LocalDate.of(2022, 1, 1))
                    sluttdato(LocalDate.of(2023, 5, 1))
                    fodselsnummer("12345678911")
                    referanse("MEL-1")
                    referanseNAV("Referanse NAV")
                    referanseBruker("Referanse bruker")
                }
            )
        }
        val fakturaBestiltDtoCapturingSlot = slot<FakturaBestiltDto>()
        val startDatoFaktura = faktura.fakturaLinje.minByOrNull { it.periodeFra }!!.periodeFra
        val sluttDatoFaktura = faktura.fakturaLinje.maxByOrNull { it.periodeFra }!!.periodeTil

        every {
            fakturaRepository.findByReferanseNr(faktura.referanseNr)
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
                    fakturaReferanseNr = faktura.referanseNr,
                    kredriteringFakturaRef = "",
                    referanseBruker = "Referanse bruker",
                    referanseNAV = "Referanse NAV",
                    beskrivelse = "Faktura Trygdeavgift ${startDatoFaktura.get(IsoFields.QUARTER_OF_YEAR)}.kvartal ${startDatoFaktura.year} - ${
                        sluttDatoFaktura.get(
                            IsoFields.QUARTER_OF_YEAR
                        )
                    }.kvartal ${sluttDatoFaktura.year}",
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

        fakturaBestillingService.bestillFaktura(faktura.referanseNr)
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
}
