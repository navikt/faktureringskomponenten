package no.nav.faktureringskomponenten.service

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import no.nav.faktureringskomponenten.domain.models.forTest
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

    private val fakturaBestillingService =
        FakturaBestillingService(fakturaRepository, fakturaserieRepository, fakturaBestiltProducer)

    @Test
    fun `Bestiller bestillingsklare faktura og lagrer i databasen`() {
        val faktura = Faktura.forTest {
            fakturaserie = Fakturaserie.forTest {
                id = 10
            }
            fakturaLinje {
                // Default test fakturalinje
            }
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
        val faktura = Faktura.forTest {
            datoBestilt = LocalDate.of(2022, 5, 1)
            fakturaLinje {
                antall = BigDecimal.ONE
                månedspris = 18000
                belop = BigDecimal(90000)
                fra = "2022-01-01"
                til = "2022-05-01"
                beskrivelse = "En beskrivelse"
            }
            fakturaserie = Fakturaserie.forTest {
                id = 10
                fullmektig = Fullmektig(fodselsnummer = "12129012345", organisasjonsnummer = "")
                fra = "2022-01-01"
                til = "2023-05-01"
                fodselsnummer = "12345678911"
                referanse = "MEL-1"
                referanseNAV = "Referanse NAV"
                referanseBruker = "Referanse bruker"
            }
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
                    krediteringFakturaRef = "",
                    referanseBruker = "Referanse bruker",
                    referanseNAV = "Referanse NAV",
                    beskrivelse = "Faktura Trygdeavgift ${startDatoFaktura.get(IsoFields.QUARTER_OF_YEAR)}.kvartal ${startDatoFaktura.year} - ${
                        sluttDatoFaktura.get(
                            IsoFields.QUARTER_OF_YEAR
                        )
                    }.kvartal ${sluttDatoFaktura.year}",
                    artikkel = "F00008",
                    faktureringsDato = LocalDate.now(),
                    fakturaLinjer = listOf(
                        FakturaBestiltLinjeDto(
                            beskrivelse = "En beskrivelse",
                            antall = BigDecimal(1),
                            enhetspris = BigDecimal(18000).setScale(2),
                            belop = BigDecimal(90000).setScale(2)
                        )
                    )
                )
            )
        }

        fakturaBestillingService.bestillFaktura(faktura.referanseNr)
    }

    @Test
    fun `Kreditnota sendes til OEBS - serie og faktura får oppdatert status`() {
        val fakturaserie = Fakturaserie.forTest {
            faktura {
                krediteringFakturaRef = ULID.randomULID()
                fakturaLinje {
                    // Default test fakturalinje
                }
            }
            faktura {
                krediteringFakturaRef = ULID.randomULID()
                fakturaLinje {
                    // Default test fakturalinje
                }
            }
        }

        every { fakturaserieRepository.findByReferanse(fakturaserie.referanse) } returns fakturaserie
        every { fakturaserieRepository.save(any()) } returns mockk()


        fakturaBestillingService.bestillKreditnota(fakturaserie)


        fakturaserie.run {
            status.shouldBe(FakturaserieStatus.FERDIG)
            faktura.run {
                shouldHaveSize(2)
                forEach {
                    it.status.shouldBe(FakturaStatus.BESTILT)
                }
            }
        }

        verify(exactly = 2) { fakturaBestiltProducer.produserBestillingsmelding(any()) }
    }
}
