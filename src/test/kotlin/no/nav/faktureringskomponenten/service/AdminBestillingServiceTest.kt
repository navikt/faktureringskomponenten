package no.nav.faktureringskomponenten.service

import io.kotest.matchers.collections.shouldBeEmpty
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
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
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

        val resultat = adminBestillingService.bestillBestillingsklareFakturaer(bestillingsDato).shouldNotBeNull()

        resultat.bestilte shouldBe fakturaer.map { it.referanseNr }
        resultat.feilede.shouldBeEmpty()
        fakturaer.forEach { verify(exactly = 1) { fakturaBestillingService.bestillFaktura(it.referanseNr) } }
    }

    @Test
    fun `bestiller ingenting på nytt når fakturaene allerede er bestilt`() {
        gittAtLåsenErLedig()
        val faktura = Faktura.forTest { }
        every { fakturaBestillingService.hentBestillingsklareFaktura(any()) } returns listOf(faktura) andThen emptyList()

        adminBestillingService.bestillBestillingsklareFakturaer()
            .shouldNotBeNull().bestilte shouldBe listOf(faktura.referanseNr)

        // Andre kjøring: fakturaen er ikke lenger bestillingsklar
        val andreKjøring = adminBestillingService.bestillBestillingsklareFakturaer().shouldNotBeNull()
        andreKjøring.bestilte.shouldBeEmpty()
        andreKjøring.feilede.shouldBeEmpty()
        verify(exactly = 1) { fakturaBestillingService.bestillFaktura(faktura.referanseNr) }
    }

    @Test
    fun `fortsetter med de øvrige fakturaene når én bestilling feiler`() {
        gittAtLåsenErLedig()
        val feilende = Faktura.forTest { }
        val vellykket = Faktura.forTest { }
        every { fakturaBestillingService.hentBestillingsklareFaktura(any()) } returns listOf(feilende, vellykket)
        every { fakturaBestillingService.bestillFaktura(feilende.referanseNr) } throws
            RessursIkkeFunnetException(field = "fakturaReferanseNr", message = "Finner ikke faktura")

        val resultat = adminBestillingService.bestillBestillingsklareFakturaer().shouldNotBeNull()

        resultat.bestilte shouldBe listOf(vellykket.referanseNr)
        resultat.feilede.map { it.fakturaReferanse } shouldBe listOf(feilende.referanseNr)
        verify(exactly = 1) { fakturaBestillingService.bestillFaktura(vellykket.referanseNr) }
    }

    @Test
    fun `returnerer tomt resultat når ingen fakturaer er klare for bestilling`() {
        gittAtLåsenErLedig()
        every { fakturaBestillingService.hentBestillingsklareFaktura(any()) } returns emptyList()

        val resultat = adminBestillingService.bestillBestillingsklareFakturaer().shouldNotBeNull()

        resultat.bestilte.shouldBeEmpty()
        resultat.feilede.shouldBeEmpty()
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
