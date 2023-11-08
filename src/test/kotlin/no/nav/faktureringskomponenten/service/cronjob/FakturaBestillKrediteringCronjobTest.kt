package no.nav.faktureringskomponenten.service.cronjob

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.service.FakturaBestillingService
import org.junit.jupiter.api.Test
import ulid.ULID
import java.time.LocalDate

class FakturaBestillKrediteringCronjobTest {

    private val fakturaBestillingService = mockk<FakturaBestillingService>(relaxed = true)
    private val fakturaBestillKrediteringCronjob = FakturaBestillKrediteringCronjob(fakturaBestillingService)

    @Test
    fun `bestillFaktura henter faktura klar for kreditering og bestiller`() {
        val fakturaReferanseNr1 = ULID.randomULID()
        val fakturaReferanseNr2 = ULID.randomULID()

        val fakturaKreditReferanseNr1 = ULID.randomULID()
        val fakturaKreditReferanseNr2 = ULID.randomULID()

        val listeAvFaktura = listOf(
            Faktura(id = 1, referanseNr = fakturaReferanseNr1, kreditReferanseNr = fakturaKreditReferanseNr1, datoBestilt = LocalDate.now(), fakturaLinje = listOf()),
            Faktura(id = 2, referanseNr = fakturaReferanseNr2, kreditReferanseNr = fakturaKreditReferanseNr2, datoBestilt = LocalDate.now(), fakturaLinje = listOf())
        )
        every { fakturaBestillingService.hentKrediteringsklareFaktura() } returns listeAvFaktura

        fakturaBestillKrediteringCronjob.krediterFaktura()

        verify {
            fakturaBestillingService.krediterFaktura(fakturaReferanseNr1)
            fakturaBestillingService.krediterFaktura(fakturaReferanseNr2)
        }
    }
}
