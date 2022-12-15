package no.nav.faktureringskomponenten.service.cronjob

import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.service.FakturaService
import java.time.LocalDate

class FakturaBestillCronjobTest : FunSpec({

    val fakturaService = mockk<FakturaService>(relaxed = true)
    val fakturaBestillCronJob = FakturaBestillCronjob(fakturaService)

    context("bestillFaktura") {
        test("bestillFaktura henter faktura og bestiller") {
            val listeAvFaktura = listOf(
                Faktura(id = 1, datoBestilt = LocalDate.now(), fakturaLinje = listOf()),
                Faktura(id = 2, datoBestilt = LocalDate.now(), fakturaLinje = listOf())
            )
            every { fakturaService.hentBestillingsklareFaktura(any()) } returns listeAvFaktura

            fakturaBestillCronJob.bestillFaktura()

            verify {
                fakturaService.bestillFaktura(1)
                fakturaService.bestillFaktura(2)
            }
        }
    }
})
