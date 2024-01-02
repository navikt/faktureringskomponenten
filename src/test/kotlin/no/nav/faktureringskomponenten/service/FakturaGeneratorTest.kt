package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
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
    private val generator = FakturaGenerator(fakturaLinjeGenerator, unleash, 0)

    private val nesteÅr = LocalDate.now().plusYears(1).year

    @AfterEach
    fun `Remove RandomNumberGenerator mockks`() {
        unmockkStatic(LocalDate::class)
    }

    @Test
    fun `PeriodeStart på faktura tilbake i tid - DatoBestilt settes til dagens dato`() {
        val faktura = generator.lagFakturaerFor(
            LocalDate.now().minusDays(1),
            LocalDate.now().plusMonths(3),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.now().minusDays(1),
                    sluttDato = LocalDate.now().plusMonths(3),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaserieIntervall.KVARTAL
        )
        faktura.first().datoBestilt.shouldBe(LocalDate.now())
    }

    @Test
    fun `PeriodeStart på faktura frem i tid - DatoBestilt settes til 19 i måneden før kvartalet perioden gjelder for`() {
        val begynnelseAvDesember = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns begynnelseAvDesember

        val faktura = generator.lagFakturaerFor(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 5, 20),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2024, 3, 31),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(15000),
                    startDato = LocalDate.of(2024, 4, 1),
                    sluttDato = LocalDate.of(2024, 5, 20),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaserieIntervall.KVARTAL
        )
        faktura.sortedBy { it.datoBestilt }.map { it.datoBestilt }
            .shouldContainInOrder(LocalDate.of(2023, 12, 19), LocalDate.of(2024, 3, 19))
    }

    @Test
    fun `PeriodeStart på faktura frem i tid, men i inneværende kvartal - DatoBestilt settes til dagens dato`() {
        val begynnelseAvDesember = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns begynnelseAvDesember

        val faktura = generator.lagFakturaerFor(
            begynnelseAvDesember.plusDays(4),
            begynnelseAvDesember.plusDays(20),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = begynnelseAvDesember.plusDays(4),
                    sluttDato = begynnelseAvDesember.plusDays(20),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.single().datoBestilt.shouldBe(LocalDate.now())
    }

    @Test
    fun `PeriodeStart på faktura frem i tid, i samme kvartal, men neste år - DatoBestilt settes til 19 i måneden før kvartalet`() {
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
    fun `PeriodeStart på faktura er i neste kvartal, men dages dato er etter kvartalskjøring - DatoBestilt settes til dagens dato`() {
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

    @Test
    fun `PeriodeStart på faktura er i neste kvartal, men dages dato er etter kvartalskjøring - over flere år fremover - DatoBestilt settes til dagens dato`() {
        val etter19SisteMånedIKvartal = LocalDate.of(2023, 12, 23)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns etter19SisteMånedIKvartal

        val faktura = generator.lagFakturaerFor(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2027, 3, 31),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2027, 3, 31),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.shouldHaveSize(13)
            .sortedBy { it.datoBestilt }
            .map { it.datoBestilt.toString() }
            .shouldContainInOrder(
                "2023-12-23",
                "2024-03-19",
                "2024-06-19",
                "2024-09-19",
                "2024-12-19",
                "2025-03-19",
                "2025-06-19",
                "2025-09-19",
                "2025-12-19",
                "2026-03-19",
                "2026-06-19",
                "2026-09-19",
                "2026-12-19",
            )
    }
}