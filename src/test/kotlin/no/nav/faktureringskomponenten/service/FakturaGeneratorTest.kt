package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate


class FakturaGeneratorTest {
    private val fakturaLinjeGenerator = FakturaLinjeGenerator()
    private val unleash = FakeUnleash()
    private val generator = FakturaGenerator(fakturaLinjeGenerator, unleash, 0)

    @AfterEach
    fun `Remove RandomNumberGenerator mockks`() {
        unmockkStatic(LocalDate::class)
    }

    @Test
    fun `Periode har opphold - setter ikke faktura for oppholdet`() {
        val faktura = generator.lagFakturaerFor(
            FakturaserieGenerator.genererPeriodisering(
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2022-12-31"),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                FakturaseriePeriode(
                    BigDecimal(1000),
                    LocalDate.parse("2020-01-01"),
                    LocalDate.parse("2020-12-31"),
                    "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
                ),
                FakturaseriePeriode(
                    BigDecimal(1000),
                    LocalDate.parse("2022-01-01"),
                    LocalDate.parse("2022-12-31"),
                    "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"
                )
            )
        )


        faktura.shouldHaveSize(2).run {
            first().fakturaLinje
                .shouldNotBeEmpty()
                .onEach {
                    it.periodeFra.year.shouldNotBe(2021)
                    it.periodeTil.year.shouldNotBe(2021)
                }
            last().fakturaLinje
                .shouldNotBeEmpty()
                .onEach {
                    it.periodeFra.year.shouldNotBe(2021)
                    it.periodeTil.year.shouldNotBe(2021)
                }
        }
    }

    @Test
    fun `PeriodeStart på faktura tilbake i tid - DatoBestilt settes til dagens dato`() {
        val faktura = generator.lagFakturaerFor(
            FakturaserieGenerator.genererPeriodisering(
                LocalDate.now().minusDays(1),
                LocalDate.now().plusMonths(3),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.now().minusDays(1),
                    sluttDato = LocalDate.now().plusMonths(3),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            )
        )
        faktura.first().datoBestilt.shouldBe(LocalDate.now())
    }

    @Test
    fun `PeriodeStart på faktura frem i tid - DatoBestilt settes til 19 i måneden før kvartalet perioden gjelder for`() {
        val begynnelseAvDesember = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns begynnelseAvDesember

        val faktura = generator.lagFakturaerFor(
            FakturaserieGenerator.genererPeriodisering(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 5, 20),
                FakturaserieIntervall.KVARTAL
            ),
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
            )
        )
        faktura.sortedBy { it.datoBestilt }.map { it.datoBestilt }
            .shouldContainInOrder(LocalDate.of(2023, 12, 19), LocalDate.of(2024, 3, 19))
    }

    @Test
    fun `Dagens dato etter kvartalskjøring, DatoBestilt settes til dagen etterpå`() {
        val etterKvartal1kjøring2024 = LocalDate.of(2024, 3, 22)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns etterKvartal1kjøring2024

        val faktura = generator.lagFakturaerFor(
            FakturaserieGenerator.genererPeriodisering(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 12, 31),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2024, 3, 31),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2024, 4, 1),
                    sluttDato = LocalDate.of(2024, 6, 30),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2024, 7, 1),
                    sluttDato = LocalDate.of(2024, 9, 30),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(15000),
                    startDato = LocalDate.of(2024, 10, 1),
                    sluttDato = LocalDate.of(2024, 12, 31),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2025, 1, 1),
                    sluttDato = LocalDate.of(2025, 3, 31),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2025, 4, 1),
                    sluttDato = LocalDate.of(2025, 6, 30),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2025, 7, 1),
                    sluttDato = LocalDate.of(2025, 9, 30),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(15000),
                    startDato = LocalDate.of(2025, 10, 1),
                    sluttDato = LocalDate.of(2025, 12, 31),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            )
        )

        faktura.sortedBy { it.datoBestilt }.map { it.datoBestilt }
            .shouldContainInOrder(
                LocalDate.of(2024, 3, 22),
                LocalDate.of(2024, 3, 22),
                LocalDate.of(2024, 6, 19),
                LocalDate.of(2024, 9, 19),
                LocalDate.of(2024, 12, 19),
                LocalDate.of(2025, 3, 19),
                LocalDate.of(2025, 6, 19),
                LocalDate.of(2025, 9, 19),
            )
    }

    @Test
    fun `PeriodeStart på faktura frem i tid, men i inneværende kvartal - DatoBestilt settes til dagens dato`() {
        val begynnelseAvDesember = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns begynnelseAvDesember

        val faktura = generator.lagFakturaerFor(
            FakturaserieGenerator.genererPeriodisering(
                begynnelseAvDesember.plusDays(4),
                begynnelseAvDesember.plusDays(20),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = begynnelseAvDesember.plusDays(4),
                    sluttDato = begynnelseAvDesember.plusDays(20),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
            )
        )

        faktura.single().datoBestilt.shouldBe(LocalDate.now())
    }

    @Test
    fun `PeriodeStart på faktura frem i tid, i samme kvartal, men neste år - DatoBestilt settes til 19 i måneden før kvartalet`() {
        val begynnelseAvDesember = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns begynnelseAvDesember

        val faktura = generator.lagFakturaerFor(
            FakturaserieGenerator.genererPeriodisering(
                begynnelseAvDesember.plusYears(1),
                begynnelseAvDesember.plusYears(1).plusDays(1),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = begynnelseAvDesember.plusYears(1),
                    sluttDato = begynnelseAvDesember.plusYears(1).plusDays(1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
            )
        )

        faktura.single().datoBestilt.shouldBe(LocalDate.of(2024, 9, 19))
    }

    @Test
    fun `PeriodeStart på faktura er i neste kvartal, men dages dato er etter kvartalskjøring - DatoBestilt settes til dagens dato`() {
        val etter19SisteMånedIKvartal = LocalDate.of(2023, 12, 24)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns etter19SisteMånedIKvartal

        val faktura = generator.lagFakturaerFor(
            FakturaserieGenerator.genererPeriodisering(
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 3, 31),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2024, 2, 1),
                    sluttDato = LocalDate.of(2024, 3, 31),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
            )
        )

        faktura.single().datoBestilt.shouldBe(etter19SisteMånedIKvartal)
    }

    @Test
    fun `PeriodeStart på faktura er i neste kvartal, men dages dato er etter kvartalskjøring - over flere år fremover - DatoBestilt settes til dagens dato`() {
        val etter19SisteMånedIKvartal = LocalDate.of(2023, 12, 23)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns etter19SisteMånedIKvartal

        val faktura = generator.lagFakturaerFor(
            FakturaserieGenerator.genererPeriodisering(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2027, 3, 31),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2027, 3, 31),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
            )
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
