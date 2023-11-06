package no.nav.faktureringskomponenten.service.cronjob

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import org.junit.jupiter.api.Test
import ulid.ULID
import java.time.LocalDate

class FakturaBestillCronjobTest {

    private val fakturaBestillingService = mockk<FakturaBestillingService>(relaxed = true)
    private val fakturaBestillCronJob = FakturaBestillCronjob(fakturaBestillingService)

    @Test
    fun `bestillFaktura henter faktura og bestiller`() {
        val fakturaReferanseNr1 = ULID.randomULID()
        val fakturaReferanseNr2 = ULID.randomULID()

        val listeAvFaktura = listOf(
            Faktura(id = 1, referanseNr = fakturaReferanseNr1, datoBestilt = LocalDate.now(), fakturaLinje = listOf()),
            Faktura(id = 2, referanseNr = fakturaReferanseNr2, datoBestilt = LocalDate.now(), fakturaLinje = listOf())
        )
        every { fakturaBestillingService.hentBestillingsklareFaktura(any()) } returns listeAvFaktura

        fakturaBestillCronJob.bestillFaktura()

        verify {
            fakturaBestillingService.bestillFaktura(fakturaReferanseNr1)
            fakturaBestillingService.bestillFaktura(fakturaReferanseNr2)
        }
    }
}
