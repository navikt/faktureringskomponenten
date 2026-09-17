package no.nav.faktureringskomponenten.service

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.core.SimpleLock
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.forTest
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional

class AdminBestillingServiceTest {

    private val fakturaBestillingService = mockk<FakturaBestillingService>(relaxed = true)
    private val lockProvider = mockk<LockProvider>()

    private val adminBestillingService = AdminBestillingService(fakturaBestillingService, lockProvider)

    @Test
    fun `bestiller alle bestillingsklare fakturaer og returnerer referansenumrene`() {
        gittAtLåsenErLedig()
        val bestillingsDato = LocalDate.of(2026, 9, 17)
        val fakturaer = listOf(Faktura.forTest { }, Faktura.forTest { })
        every { fakturaBestillingService.hentBestillingsklareFaktura(bestillingsDato) } returns fakturaer

        val bestilte = adminBestillingService.bestillBestillingsklareFakturaer(bestillingsDato).shouldNotBeNull()

        bestilte shouldBe fakturaer.map { it.referanseNr }
        fakturaer.forEach { verify(exactly = 1) { fakturaBestillingService.bestillFaktura(it.referanseNr) } }
    }

    @Test
    fun `returnerer tom liste når ingen fakturaer er klare for bestilling`() {
        gittAtLåsenErLedig()
        every { fakturaBestillingService.hentBestillingsklareFaktura(any()) } returns emptyList()

        adminBestillingService.bestillBestillingsklareFakturaer().shouldNotBeNull() shouldBe emptyList()

        verify(exactly = 0) { fakturaBestillingService.bestillFaktura(any()) }
    }

    @Test
    fun `bestiller ingenting når bestilling allerede kjører`() {
        every { lockProvider.lock(any()) } returns Optional.empty()

        adminBestillingService.bestillBestillingsklareFakturaer().shouldBeNull()

        verify(exactly = 0) { fakturaBestillingService.hentBestillingsklareFaktura(any()) }
        verify(exactly = 0) { fakturaBestillingService.bestillFaktura(any()) }
    }

    private fun gittAtLåsenErLedig() {
        every { lockProvider.lock(any()) } returns Optional.of(mockk<SimpleLock>(relaxed = true))
    }
}
