package no.nav.faktureringskomponenten.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verifySequence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltLinjeDto
import no.nav.faktureringskomponenten.testutils.FakturaUtil
import java.math.BigDecimal
import java.time.LocalDate

class FakturaServiceTest : FunSpec({

    val fakturaRepository = mockk<FakturaRepository>(relaxed = true)
    val fakturaserieRepository = mockk<FakturaserieRepository>(relaxed = true)
    val fakturaBestiltProducer = mockk<FakturaBestiltProducer>(relaxed = true)

    val fakturaService = FakturaService(fakturaRepository, fakturaserieRepository, fakturaBestiltProducer)

    test("Bestiller bestillingsklare faktura") {
        val faktura = FakturaUtil.lagFaktura(1)

        every {
            fakturaRepository.findById(1)
        } returns faktura

        every {
            fakturaserieRepository.save(any())
        } returns faktura.fakturaserie

        every {
            fakturaRepository.save(any())
        } returns faktura

        withContext(Dispatchers.IO) {
            fakturaService.bestillFaktura(1)
        }

        verifySequence {
            fakturaBestiltProducer.produserBestillingsmelding(any())
            fakturaserieRepository.save(faktura.fakturaserie)
            fakturaRepository.save(faktura)
        }
    }

    test("verifiser bestillingsmelding får riktig data") {
        val faktura = FakturaUtil.lagFaktura(1)
        val fakturaBestiltDtoCapturingSlot = slot<FakturaBestiltDto>()

        every {
            fakturaRepository.findById(1)
        } returns faktura

        every {
            fakturaserieRepository.save(any())
        } returns faktura.fakturaserie

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
                    vedtaksnummer = "MEL-1",
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
                            enhetspris = BigDecimal(90000),
                            belop = BigDecimal(90000)
                        )
                    )
                )
            )
        }

        withContext(Dispatchers.IO) {
            fakturaService.bestillFaktura(1)
        }
    }
})