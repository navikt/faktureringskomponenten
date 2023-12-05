package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate


class FakturaGeneratorTest {
    private val fakturaLinjeGenerator = mockk<FakturaLinjeGenerator>(relaxed = true)
    private val unleash = FakeUnleash()
    private val generator = FakturaGenerator(fakturaLinjeGenerator, unleash)

    private val nesteÅr = LocalDate.now().plusYears(1).year

    @AfterEach
    fun `Remove RandomNumberGenerator mockks`() {
        unmockkStatic(LocalDate::class)
    }

    @Test
    fun `DatoBestilt på faktura tilbake i tid settes til dagens dato hvis periode starter før dagens dato`() {
        val faktura = generator.lagFakturaerFor(
            LocalDate.now(),
            LocalDate.now().plusMonths(3),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now().plusMonths(3),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaserieIntervall.KVARTAL
        )
        faktura.first().datoBestilt.shouldBe(LocalDate.now())
    }

    @Test
    fun `DatoBestilt på faktura frem i tid settes til 19 i måneden før kvartalet perioden gjelder for`() {
        val faktura = generator.lagFakturaerFor(
            LocalDate.of(nesteÅr, 1, 1),
            LocalDate.of(nesteÅr, 5, 20),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(nesteÅr, 1, 1),
                    sluttDato = LocalDate.of(nesteÅr, 3, 31),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(15000),
                    startDato = LocalDate.of(nesteÅr, 4, 1),
                    sluttDato = LocalDate.of(nesteÅr, 5, 20),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaserieIntervall.KVARTAL
        )
        faktura.sortedBy { it.datoBestilt }.map { it.datoBestilt }
            .shouldContainInOrder(LocalDate.of(nesteÅr - 1, 12, 19), LocalDate.of(nesteÅr, 3, 19))
    }

    @Test
    fun `DatoBestilt på faktura frem i tid, men i inneværende kvartal - settes til dagens dato`() {
        val faktura = generator.lagFakturaerFor(
            LocalDate.now().plusDays(3),
            LocalDate.now().plusDays(20),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.now().plusDays(3),
                    sluttDato = LocalDate.now().plusDays(20),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.single().datoBestilt.shouldBe(LocalDate.now())
    }

    @Test
    fun `DatoBestilt på faktura frem i tid, i samme kvartal, men neste år - settes til 19 i måneden før kvartalet`() {
        val faktura = generator.lagFakturaerFor(
            LocalDate.now().plusYears(1),
            LocalDate.now().plusYears(1).plusDays(1),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.now().plusYears(1),
                    sluttDato = LocalDate.now().plusYears(1).plusDays(1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
            ),
            FakturaserieIntervall.KVARTAL
        )

        val firstMonthOfQuarter = LocalDate.now().month.firstMonthOfQuarter()
        faktura.single().datoBestilt.shouldBe(
            LocalDate.now().plusYears(1).withMonth(firstMonthOfQuarter.value - 1).withDayOfMonth(19)
        )
    }

    @Test
    fun `DatoBestilt på faktura er i neste kvartal, men dages dato er etter kvartalskjøring - settes til dagens dato`() {
        val etter19SisteMånedIKvartal = LocalDate.of(2023, 12, 24)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns etter19SisteMånedIKvartal

        val faktura = generator.lagFakturaerFor(
            LocalDate.of(2024, 2, 1),
            LocalDate.of(2024, 3, 31),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2024, 2, 1),
                    sluttDato = LocalDate.of(2024, 3, 31),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.single().datoBestilt.shouldBe(etter19SisteMånedIKvartal)
    }
}