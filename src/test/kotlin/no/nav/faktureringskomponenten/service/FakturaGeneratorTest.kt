package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
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
import no.nav.faktureringskomponenten.domain.models.forTest
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

    // Helper functions to reduce test boilerplate
    private fun kvartalsPerioder(
        startÅr: Int,
        sluttÅr: Int,
        månedspris: Int,
        beskrivelse: String,
        q4Månedspris: Int? = null // Q4 kan ha annen pris
    ): List<FakturaseriePeriode> {
        val perioder = mutableListOf<FakturaseriePeriode>()

        for (år in startÅr..sluttÅr) {
            // Q1: jan-mar
            perioder.add(FakturaseriePeriode.forTest {
                this.månedspris = månedspris
                fra = "$år-01-01"
                til = "$år-03-31"
                this.beskrivelse = beskrivelse
            })
            // Q2: apr-jun
            perioder.add(FakturaseriePeriode.forTest {
                this.månedspris = månedspris
                fra = "$år-04-01"
                til = "$år-06-30"
                this.beskrivelse = beskrivelse
            })
            // Q3: jul-sep
            perioder.add(FakturaseriePeriode.forTest {
                this.månedspris = månedspris
                fra = "$år-07-01"
                til = "$år-09-30"
                this.beskrivelse = beskrivelse
            })
            // Q4: okt-des (kan ha annen pris)
            perioder.add(FakturaseriePeriode.forTest {
                this.månedspris = q4Månedspris ?: månedspris
                fra = "$år-10-01"
                til = "$år-12-31"
                this.beskrivelse = beskrivelse
            })
        }

        return perioder
    }

    private fun periode(månedspris: Int, fra: String, til: String, beskrivelse: String) =
        FakturaseriePeriode.forTest {
            this.månedspris = månedspris
            this.fra = fra
            this.til = til
            this.beskrivelse = beskrivelse
        }

    @Test
    fun `Periode har opphold - setter ikke faktura for oppholdet`() {
        val faktura = generator.lagFakturaerFor(
            FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2022-12-31"),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                periode(1000, "2020-01-01", "2020-12-31", "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%"),
                periode(1000, "2022-01-01", "2022-12-31", "Inntekt: 10000, Dekning: Pensjon og helsedel, Sats 10%")
            ),
            FakturaserieIntervall.KVARTAL
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
            FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.now().minusDays(1),
                LocalDate.now().plusMonths(3),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 25470
                    startDato = LocalDate.now().minusDays(1)
                    sluttDato = LocalDate.now().plusMonths(3)
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                }
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
            FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 5, 20),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                periode(25470, "2024-01-01", "2024-03-31", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"),
                periode(15000, "2024-04-01", "2024-05-20", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            ),
            FakturaserieIntervall.KVARTAL
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
            FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2025, 12, 31),
                FakturaserieIntervall.KVARTAL
            ),
            kvartalsPerioder(
                startÅr = 2024,
                sluttÅr = 2025,
                månedspris = 25470,
                q4Månedspris = 15000,
                beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
            ),
            FakturaserieIntervall.KVARTAL
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
            FakturaIntervallPeriodisering.genererPeriodisering(
                begynnelseAvDesember.plusDays(4),
                begynnelseAvDesember.plusDays(20),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 25470
                    startDato = begynnelseAvDesember.plusDays(4)
                    sluttDato = begynnelseAvDesember.plusDays(20)
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                },
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.single().datoBestilt.shouldBe(LocalDate.now())
    }

    @Test
    fun `PeriodeStart på faktura frem i tid, i samme kvartal, men neste år - DatoBestilt settes til 19 i måneden før kvartalet`() {
        val begynnelseAvDesember = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns begynnelseAvDesember

        val faktura = generator.lagFakturaerFor(
            FakturaIntervallPeriodisering.genererPeriodisering(
                begynnelseAvDesember.plusYears(1),
                begynnelseAvDesember.plusYears(1).plusDays(1),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 25470
                    startDato = begynnelseAvDesember.plusYears(1)
                    sluttDato = begynnelseAvDesember.plusYears(1).plusDays(1)
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                },
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.single().datoBestilt.shouldBe(LocalDate.of(2024, 9, 19))
    }

    @Test
    fun `PeriodeStart på faktura er i neste kvartal, men dages dato er etter kvartalskjøring - DatoBestilt settes til dagens dato`() {
        val etter19SisteMånedIKvartal = LocalDate.of(2023, 12, 24)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns etter19SisteMånedIKvartal

        val faktura = generator.lagFakturaerFor(
            FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.of(2024, 2, 1),
                LocalDate.of(2024, 3, 31),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 25470
                    fra = "2024-02-01"
                    til = "2024-03-31"
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                },
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.single().datoBestilt.shouldBe(etter19SisteMånedIKvartal)
    }


    @Test
    fun `skal lage en faktura per år for historiske perioder selv om det ikke er siste dag i året`() {
        val førsteKvartal2023 = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns førsteKvartal2023

        val faktura = generator.lagFakturaerFor(
            periodisering = FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2022, 11, 30),  // Merk: Ikke siste dag i året
                FakturaserieIntervall.KVARTAL
            ),
            fakturaseriePerioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 1000
                    fra = "2020-01-01"
                    til = "2022-12-31"
                    beskrivelse = "Test periode"
                }
            ),
            FakturaserieIntervall.KVARTAL
        )

        // Skal få 3 fakturaer (2020, 2021, 2022) selv om siste periode ikke er på slutten av året
        faktura.shouldHaveSize(3).run {
            map { it.fakturaLinje.first().periodeFra.year }.shouldContainInOrder(2020, 2021, 2022)
        }
    }

    @Test
    fun `tom periodisering gir tom liste med fakturaer`() {
        val faktura = generator.lagFakturaerFor(
            periodisering = emptyList(),
            fakturaseriePerioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 1000
                    fra = "2020-01-01"
                    til = "2020-12-31"
                    beskrivelse = "Test periode"
                }
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.shouldHaveSize(0)
    }

    @Test
    fun `tomme fakturaseriePerioder gir tom liste med fakturaer`() {
        val faktura = generator.lagFakturaerFor(
            periodisering = FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2020-12-31"),
                FakturaserieIntervall.KVARTAL
            ),
            fakturaseriePerioder = emptyList(),
            FakturaserieIntervall.KVARTAL
        )

        faktura.shouldHaveSize(0)
    }

    @Test
    fun `fremtidige perioder med opphold - setter ikke faktura for oppholdet`() {
        val dagensDato = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns dagensDato

        val faktura = generator.lagFakturaerFor(
            FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2026, 12, 31),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                periode(1000, "2024-01-01", "2024-12-31", "Første periode"),
                periode(1000, "2026-01-01", "2026-12-31", "Andre periode")
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.flatMap { it.fakturaLinje }
            .map { it.periodeFra.year }
            .distinct()
            .shouldContainInOrder(2024, 2026) // Skal ikke inneholde 2025
    }

    @Test
    fun `skal feile når periodisering går utenfor fakturaseriePerioder`() {
        val dagensDato = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns dagensDato

        shouldThrow<IllegalArgumentException> {
            generator.lagFakturaerFor(
                periodisering = listOf(
                    LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 12, 31),
                ),
                fakturaseriePerioder = listOf(
                    FakturaseriePeriode.forTest {
                    månedspris = 1000
                    fra = "2024-01-01"
                    til = "2024-06-30"
                    beskrivelse = "Test periode"
                }
                ),
                FakturaserieIntervall.KVARTAL
            )
        }.message shouldBe  "Periodisering (2024-01-01 til 2024-12-31) må være innenfor faktureringsperiodene (2024-01-01 til 2024-06-30)"
    }

    @Test
    fun `alle perioder er i fremtiden - skal lage separate fakturaer per periode`() {
        val dagensDato = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns dagensDato

        val periodisering = FakturaIntervallPeriodisering.genererPeriodisering(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
            FakturaserieIntervall.KVARTAL
        )

        val faktura = generator.lagFakturaerFor(
            periodisering = periodisering,
            fakturaseriePerioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 1000
                    fra = "2024-01-01"
                    til = "2024-12-31"
                    beskrivelse = "Test periode"
                }
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.shouldHaveSize(4) // Ett per kvartal
    }

    @Test
    fun `alle perioder er historiske - skal grupperes per år`() {
        val dagensDato = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns dagensDato

        val periodisering = FakturaIntervallPeriodisering.genererPeriodisering(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2022, 12, 31),
            FakturaserieIntervall.KVARTAL
        )

        val faktura = generator.lagFakturaerFor(
            periodisering = periodisering,
            fakturaseriePerioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 1000
                    fra = "2020-01-01"
                    til = "2022-12-31"
                    beskrivelse = "Test periode"
                }
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.shouldHaveSize(3) // En per år (2020, 2021, 2022)
    }

    @Test
    fun `overlappende fakturaseriePerioder - skal håndtere overlapp korrekt`() {
        val dagensDato = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns dagensDato

        val startDato = LocalDate.of(2024, 1, 1)
        val overlappStartDato = LocalDate.of(2024, 3, 15)
        val sluttDatoFørstePeriode = LocalDate.of(2024, 3, 31)
        val sluttDato = LocalDate.of(2024, 6, 30)

        val beløpFørstePeriode = BigDecimal(1000)
        val beløpAndrePeriode = BigDecimal(2000)

        // Act
        val fakturaer = generator.lagFakturaerFor(
            periodisering = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.KVARTAL
            ),
            fakturaseriePerioder = listOf(
                FakturaseriePeriode.forTest {
                    enhetsprisPerManed = beløpFørstePeriode
                    this.startDato = startDato
                    this.sluttDato = sluttDatoFørstePeriode
                    beskrivelse = "Første periode"
                },
                FakturaseriePeriode.forTest {
                    enhetsprisPerManed = beløpAndrePeriode
                    this.startDato = overlappStartDato
                    this.sluttDato = sluttDato
                    beskrivelse = "Andre periode"
                }
            ),
            FakturaserieIntervall.KVARTAL
        )

        val fakturaLinjer = fakturaer.flatMap { it.fakturaLinje }.sortedBy { it.periodeFra }

        with(fakturaLinjer) {
            size.shouldBe(3)
            // Første kvartal (1. jan - 31. mars med 1000kr)
            first().let { linje ->
                linje.periodeFra.shouldBe(startDato)
                linje.periodeTil.shouldBe(sluttDatoFørstePeriode)
                linje.enhetsprisPerManed.shouldBe(beløpFørstePeriode.setScale(2))
                // 3 måneder med 1000kr per måned
                linje.belop.toString().shouldBe("3000.00")
            }

            // Overlappende periode (15. mars - 31. mars)
            get(1).let { linje ->
                linje.periodeFra.shouldBe(overlappStartDato)
                linje.periodeTil.shouldBe(sluttDatoFørstePeriode)
                linje.enhetsprisPerManed.shouldBe(2000.toBigDecimal().setScale(2))
                linje.belop.toString().shouldBe("1100.00") // (31/16) * 2000
            }

            // Andre kvartal (1. april - 30. juni med 2000kr)
            last().let { linje ->
                linje.periodeFra.shouldBe(sluttDatoFørstePeriode.plusDays(1))
                linje.periodeTil.shouldBe(sluttDato)
                linje.enhetsprisPerManed.shouldBe(beløpAndrePeriode.setScale(2))
                // 3 måneder med 2000kr per måned
                linje.belop.toString().shouldBe("6000.00")
            }
        }
    }

    @Test
    fun `periodisering som starter midt i en måned`() {
        val dagensDato = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns dagensDato

        val faktura = generator.lagFakturaerFor(
            periodisering = FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.of(2024, 1, 15), // Merk: Starter midt i måneden
                LocalDate.of(2024, 6, 30),
                FakturaserieIntervall.KVARTAL
            ),
            fakturaseriePerioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 1000
                    fra = "2024-01-01"
                    til = "2024-06-30"
                    beskrivelse = "Test periode"
                }
            ),
            FakturaserieIntervall.KVARTAL
        )

        // Verifiser at første fakturalinjes startdato er 15. januar
        faktura.first().fakturaLinje.first().periodeFra.shouldBe(LocalDate.of(2024, 1, 15))
    }

    @Test
    fun `fakturaseriePeriode som delvis overlapper med periodisering`() {
        val dagensDato = LocalDate.of(2023, 12, 1)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns dagensDato

        val faktura = generator.lagFakturaerFor(
            periodisering = FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 3, 31),
                FakturaserieIntervall.KVARTAL
            ),
            fakturaseriePerioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 1000
                    fra = "2023-12-15" // Starter før periodisering
                    til = "2024-04-15"  // Slutter etter periodisering
                    beskrivelse = "Test periode"
                }
            ),
            FakturaserieIntervall.KVARTAL
        )

        faktura.flatMap { it.fakturaLinje }.run {
            first().periodeFra.shouldBe(LocalDate.of(2024, 1, 1)) // Skal starte med periodiseringen
            last().periodeTil.shouldBe(LocalDate.of(2024, 3, 31)) // Skal slutte med periodiseringen
        }
    }

    @Test
    fun `dagensDato er nøyaktig på grensen mellom historisk og fremtidig, produserer to faktura`() {
        val grenseDato = LocalDate.of(2024, 3, 31)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns grenseDato

        val faktura = generator.lagFakturaerFor(
            periodisering = FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 6, 30),
                FakturaserieIntervall.KVARTAL
            ),
            fakturaseriePerioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 1000
                    fra = "2024-01-01"
                    til = "2024-06-30"
                    beskrivelse = "Test periode"
                }
            ),
            FakturaserieIntervall.KVARTAL
        )

        // Første kvartal skal være historisk, andre kvartal fremtidig
        faktura.shouldHaveSize(2)
        faktura.first().fakturaLinje.first().periodeFra.shouldBe(LocalDate.of(2024, 1, 1))
        faktura.last().fakturaLinje.first().periodeFra.shouldBe(LocalDate.of(2024, 4, 1))
    }

    @Test
    fun `PeriodeStart på faktura er i neste kvartal, men dages dato er etter kvartalskjøring - over flere år fremover - DatoBestilt settes til dagens dato`() {
        val etter19SisteMånedIKvartal = LocalDate.of(2023, 12, 23)
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns etter19SisteMånedIKvartal

        val faktura = generator.lagFakturaerFor(
            FakturaIntervallPeriodisering.genererPeriodisering(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2027, 3, 31),
                FakturaserieIntervall.KVARTAL
            ),
            listOf(
                periode(25470, "2024-01-01", "2027-03-31", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
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
