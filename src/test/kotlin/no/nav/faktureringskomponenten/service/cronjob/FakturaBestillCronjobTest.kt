package no.nav.faktureringskomponenten.service.cronjob

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FakturaBestillCronjobTest {

    private val fakturaBestillingService = mockk<FakturaBestillingService>(relaxed = true)
    private val fakturaBestillCronJob = FakturaBestillCronjob(fakturaBestillingService)

    @Test
    fun `bestillFaktura henter faktura og bestiller`() {
        val listeAvFaktura = listOf(
            Faktura(id = 1, datoBestilt = LocalDate.now(), fakturaLinje = listOf()),
            Faktura(id = 2, datoBestilt = LocalDate.now(), fakturaLinje = listOf())
        )
        every { fakturaBestillingService.hentBestillingsklareFaktura(any()) } returns listeAvFaktura

        fakturaBestillCronJob.bestillFaktura()

        verify {
            fakturaBestillingService.bestillFaktura(1)
            fakturaBestillingService.bestillFaktura(2)
        }
    }
}
