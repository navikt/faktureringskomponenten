package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.faktureringskomponenten.config.ToggleName
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.service.FakturaserieGenerator.Companion.substract
import no.nav.faktureringskomponenten.service.avregning.AvregningBehandler
import no.nav.faktureringskomponenten.service.avregning.AvregningsfakturaGenerator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.threeten.extra.LocalDateRange
import ulid.ULID
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
class FakturaserieGeneratorTest {
    private val unleash: FakeUnleash = FakeUnleash()

    @AfterEach
    fun tearDown() {
        unleash.disable((ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER))
        unmockkStatic(LocalDate::class)
    }

    @ParameterizedTest(name = "[{index}] {2} {0}")
    @MethodSource("data")
    fun testFakturaLinjer(
        beskrivelse: String,
        dagensDato: LocalDate,
        intervall: FakturaserieIntervall,
        perioder: List<FakturaseriePeriode>,
        expected: ForventetFakturering
    ) {
        val fakturaserie = lagFakturaserie(dagensDato, intervall, perioder)
        val result = ForventetFakturering(fakturaserie.faktura)

        result.shouldBeEqualToComparingFields(expected)
    }

    // Test case builder - hides boilerplate and makes tests more readable
    private fun testCase(init: FakturaserieTestCaseBuilder.() -> Unit) =
        FakturaserieTestCaseBuilder().apply(init).build()

    class FakturaserieTestCaseBuilder {
        lateinit var name: String
        var dagensDato: LocalDate = LocalDate.of(2023, 1, 1)
        var intervall: FakturaserieIntervall = FakturaserieIntervall.KVARTAL
        var perioder = mutableListOf<FakturaseriePeriode>()
        var fakturaer = mutableListOf<FakturaMedLinjer>()

        fun periode(månedspris: Int, fra: String, til: String, beskrivelse: String) {
            perioder.add(FakturaseriePeriode.forTest {
                this.månedspris = månedspris
                this.fra = fra
                this.til = til
                this.beskrivelse = beskrivelse
            })
        }

        fun faktura(fra: String, til: String, init: FakturaBuilder.() -> Unit) {
            val builder = FakturaBuilder(fra, til)
            builder.init()
            fakturaer.add(builder.build())
        }

        fun build() = arguments(
            name,
            dagensDato,
            intervall,
            perioder,
            ForventetFakturering(fakturaer.size, fakturaer)
        )
    }

    class FakturaBuilder(
        private val fra: String,
        private val til: String
    ) {
        private val linjer = mutableListOf<Linje>()

        fun linje(fra: String, til: String, beløp: String, beskrivelse: String) {
            linjer.add(Linje(fra, til, beløp, beskrivelse))
        }

        fun build() = FakturaMedLinjer(fra, til, linjer)
    }

    private fun data() = listOf(
        testCase {
            name = "Går 1 dag inn i neste måned"
            dagensDato = LocalDate.of(2023, 1, 13)
            intervall = FakturaserieIntervall.MANEDLIG
            periode(
                månedspris = 25470,
                fra = "2022-11-01",
                til = "2022-12-01",
                beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
            )
            faktura(fra = "2022-11-01", til = "2022-12-01") {
                linje("2022-12-01", "2022-12-01", "764.10", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
                linje("2022-11-01", "2022-11-30", "25470.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
        },

        testCase {
            name = "Medlemskap starter i 2022, fortsetter i 2023, egen faktura for perioden før årsovergang"
            dagensDato = LocalDate.of(2023, 1, 26)
            periode(10000, "2022-06-01", "2023-01-24", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            periode(10000, "2023-01-25", "2023-06-01", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            faktura("2022-06-01", "2022-12-31") {
                linje("2022-10-01", "2022-12-31", "30000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
                linje("2022-07-01", "2022-09-30", "30000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
                linje("2022-06-01", "2022-06-30", "10000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            }
            faktura("2023-01-01", "2023-03-31") {
                linje("2023-01-25", "2023-03-31", "22300.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
                linje("2023-01-01", "2023-01-24", "7700.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            }
            faktura("2023-04-01", "2023-06-01") {
                linje("2023-04-01", "2023-06-01", "20300.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
        },

        testCase {
            name = "Datoer i samme måned"
            periode(1000, "2023-01-15", "2023-01-30", "periode - 1")
            faktura("2023-01-15", "2023-01-30") {
                linje("2023-01-15", "2023-01-30", "520.00", "periode - 1")
            }
        },

        testCase {
            name = "Slutt dato er før dagens dato"
            dagensDato = LocalDate.of(2023, 4, 1)
            intervall = FakturaserieIntervall.MANEDLIG
            periode(10000, "2023-01-01", "2023-02-01", "periode - 1")
            faktura("2023-01-01", "2023-02-01") {
                linje("2023-02-01", "2023-02-01", "400.00", "periode - 1")
                linje("2023-01-01", "2023-01-31", "10000.00", "periode - 1")
            }
        },

        testCase {
            name = "Dagens dato er lik slutt dato"
            dagensDato = LocalDate.of(2022, 12, 31)
            intervall = FakturaserieIntervall.MANEDLIG
            periode(25470, "2022-11-01", "2022-12-31", "periode 1")
            faktura("2022-11-01", "2022-12-31") {
                linje("2022-12-01", "2022-12-31", "25470.00", "periode 1")
                linje("2022-11-01", "2022-11-30", "25470.00", "periode 1")
            }
        },

        testCase {
            name = "Før og etter dagens dato"
            dagensDato = LocalDate.of(2022, 4, 13)
            intervall = FakturaserieIntervall.MANEDLIG
            periode(25470, "2022-03-01", "2022-05-01", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            faktura("2022-03-01", "2022-04-30") {
                linje("2022-04-01", "2022-04-30", "25470.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
                linje("2022-03-01", "2022-03-31", "25470.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
            faktura("2022-05-01", "2022-05-01") {
                linje("2022-05-01", "2022-05-01", "764.10", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
        },

        testCase {
            name = "2 faktura perioder - kun 1 dag i siste måned"
            dagensDato = LocalDate.of(2023, 1, 23)
            intervall = FakturaserieIntervall.MANEDLIG
            periode(25470, "2022-12-01", "2023-01-22", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            periode(25470, "2023-01-23", "2023-02-01", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            faktura("2022-12-01", "2022-12-31") {
                linje("2022-12-01", "2022-12-31", "25470.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            }
            faktura("2023-01-01", "2023-01-31") {
                linje("2023-01-23", "2023-01-31", "7386.30", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
                linje("2023-01-01", "2023-01-22", "18083.70", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            }
            faktura("2023-02-01", "2023-02-01") {
                linje("2023-02-01", "2023-02-01", "1018.80", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
        },

        testCase {
            name = "2 perioder - lag 7 fakturaer med linjer"
            dagensDato = LocalDate.of(2023, 1, 26)
            intervall = FakturaserieIntervall.MANEDLIG
            periode(10000, "2022-06-01", "2023-01-24", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            periode(10000, "2023-01-25", "2023-06-01", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            faktura("2022-06-01", "2022-12-31") {
                linje("2022-12-01", "2022-12-31", "10000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
                linje("2022-11-01", "2022-11-30", "10000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
                linje("2022-10-01", "2022-10-31", "10000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
                linje("2022-09-01", "2022-09-30", "10000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
                linje("2022-08-01", "2022-08-31", "10000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
                linje("2022-07-01", "2022-07-31", "10000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
                linje("2022-06-01", "2022-06-30", "10000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            }
            faktura("2023-01-01", "2023-01-31") {
                linje("2023-01-25", "2023-01-31", "2300.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
                linje("2023-01-01", "2023-01-24", "7700.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            }
            faktura("2023-02-01", "2023-02-28") {
                linje("2023-02-01", "2023-02-28", "10000.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
            faktura("2023-03-01", "2023-03-31") {
                linje("2023-03-01", "2023-03-31", "10000.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
            faktura("2023-04-01", "2023-04-30") {
                linje("2023-04-01", "2023-04-30", "10000.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
            faktura("2023-05-01", "2023-05-31") {
                linje("2023-05-01", "2023-05-31", "10000.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
            faktura("2023-06-01", "2023-06-01") {
                linje("2023-06-01", "2023-06-01", "300.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
        },

        testCase {
            name = "2 perioder - lag 3 fakturaer med linjer"
            dagensDato = LocalDate.of(2023, 1, 26)
            periode(10000, "2022-06-01", "2023-01-24", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            periode(10000, "2023-01-25", "2023-06-01", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            faktura("2022-06-01", "2022-12-31") {
                linje("2022-10-01", "2022-12-31", "30000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
                linje("2022-07-01", "2022-09-30", "30000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
                linje("2022-06-01", "2022-06-30", "10000.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            }
            faktura("2023-01-01", "2023-03-31") {
                linje("2023-01-25", "2023-03-31", "22300.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
                linje("2023-01-01", "2023-01-24", "7700.00", "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %")
            }
            faktura("2023-04-01", "2023-06-01") {
                linje("2023-04-01", "2023-06-01", "20300.00", "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
        }
    )

    @Test
    fun `Finnes eksisterende fakturaserie fra tidligere kalenderår - disse skal avregnes når toggle er av`() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.of(2025, 8, 27)


        val opprinneligFakturaSerie = lagFakturaserie(
            intervall = FakturaserieIntervall.KVARTAL,
            perioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 10000
                    fra = "2024-01-01"
                    til = "2025-12-31"
                    beskrivelse = "Inntekt: 10000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                }
            )
        )

        opprinneligFakturaSerie.faktura.forEach {
            it.status = FakturaStatus.BESTILT
        }

        val nyFakturaSerie = lagFakturaserieForEndring(
            intervall = FakturaserieIntervall.KVARTAL,
            perioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 5000
                    fra = "2025-01-01"
                    til = "2025-12-31"
                    beskrivelse = "Inntekt: 5000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                }
            ),
            opprinneligFakturaserie = opprinneligFakturaSerie
        )

        ForventetFakturering(nyFakturaSerie.faktura).shouldBeEqualToComparingFields(
            ForventetFakturering(
                3,
                listOf(
                    FakturaMedLinjer(
                        fra = "2025-01-01", til = "2025-09-30",
                        listOf(
                            Linje(
                                fra = LocalDate.of(2025, 1, 1),
                                til = LocalDate.of(2025, 9, 30),
                                beløp = BigDecimal("-45000.00"),
                                beskrivelse = "Periode: 01.01.2025 - 30.09.2025\nNytt beløp: 45000,00 - tidligere beløp: 90000,00"
                            )
                        )
                    ),

                    FakturaMedLinjer(
                        fra = "2025-10-01", til = "2025-12-31",
                        listOf(
                            Linje(
                                fra = LocalDate.of(2025, 10, 1),
                                til = LocalDate.of(2025, 12, 31),
                                beløp = BigDecimal("-15000.00"),
                                beskrivelse = "Periode: 01.10.2025 - 31.12.2025\nNytt beløp: 15000,00 - tidligere beløp: 30000,00"
                            )
                        )
                    ),

                    FakturaMedLinjer(
                        fra = "2024-01-01", til = "2024-12-31",
                        listOf(
                            Linje(
                                fra = LocalDate.of(2024, 1, 1),
                                til = LocalDate.of(2024, 12, 31),
                                beløp = BigDecimal("-120000.00"),
                                beskrivelse = "Periode: 01.01.2024 - 31.12.2024\nNytt beløp: 0,00 - tidligere beløp: 120000,00"
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `Finnes eksisterende fakturaserie fra tidligere kalenderår - disse skal ikke avregnes ved ny fakturaserie`() {
        unleash.apply { enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER) }
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.of(2025, 8, 27)


        val opprinneligFakturaSerie = lagFakturaserie(
            intervall = FakturaserieIntervall.KVARTAL,
            perioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 10000
                    fra = "2024-01-01"
                    til = "2025-12-31"
                    beskrivelse = "Inntekt: 10000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                }
            )
        )

        opprinneligFakturaSerie.faktura.forEach {
            it.status = FakturaStatus.BESTILT
        }

        val nyFakturaSerie = lagFakturaserieForEndring(
            intervall = FakturaserieIntervall.KVARTAL,
            perioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 5000
                    fra = "2025-01-01"
                    til = "2025-12-31"
                    beskrivelse = "Inntekt: 5000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                }
            ),
            opprinneligFakturaserie = opprinneligFakturaSerie
        )

        ForventetFakturering(nyFakturaSerie.faktura).shouldBeEqualToComparingFields(
            ForventetFakturering(
                2,
                listOf(
                    FakturaMedLinjer(
                        fra = "2025-01-01", til = "2025-09-30",
                        listOf(
                            Linje(
                                "2025-01-01", "2025-09-30", "-45000.00",
                                "Nytt beløp: 45000,00 - tidligere beløp: 90000,00"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2025-10-01", til = "2025-12-31",
                        listOf(
                            Linje(
                                "2025-10-01", "2025-12-31", "-15000.00",
                                "Nytt beløp: 15000,00 - tidligere beløp: 30000,00"
                            ),
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `Utvidet periode over maanedsgrense gir nye fakturaer fra foerste dag etter avregning`() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.of(2025, 1, 15)
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_NY_PERIODISERING)

        val opprinneligFakturaSerie = lagFakturaserie(
            intervall = FakturaserieIntervall.KVARTAL,
            perioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 1000
                    fra = "2024-01-01"
                    til = "2024-02-29"
                    beskrivelse = "Inntekt"
                }
            )
        )

        opprinneligFakturaSerie.faktura.forEach {
            it.status = FakturaStatus.BESTILT
        }

        val nyFakturaSerie = lagFakturaserieForEndring(
            intervall = FakturaserieIntervall.KVARTAL,
            perioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 2000
                    fra = "2024-01-01"
                    til = "2024-03-15"
                    beskrivelse = "Inntekt"
                }
            ),
            opprinneligFakturaserie = opprinneligFakturaSerie
        )

        val fakturaer = nyFakturaSerie.faktura
        fakturaer shouldHaveSize 2

        val sorterteFakturaer = fakturaer.sortedBy { it.getPeriodeFra() }

        // Avregningsfaktura for januar-februar
        sorterteFakturaer[0].run {
            getPeriodeFra() shouldBe LocalDate.of(2024, 1, 1)
            getPeriodeTil() shouldBe LocalDate.of(2024, 2, 29)
        }

        // Ny faktura for mars 1-15
        sorterteFakturaer[1].run {
            getPeriodeFra() shouldBe LocalDate.of(2024, 3, 1)
            getPeriodeTil() shouldBe LocalDate.of(2024, 3, 15)
        }
    }

    @Test
    fun `Endring med flere avregningsperioder gir ikke dobbeltfakturering`() {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.of(2024, 1, 15)

        val opprinneligFakturaSerie = lagFakturaserie(
            intervall = FakturaserieIntervall.KVARTAL,
            perioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 1000
                    fra = "2024-01-01"
                    til = "2024-06-30"
                    beskrivelse = "Inntekt"
                }
            )
        )

        // Skal ha 2 bestilte fakturaer: Q1 + Q2
        opprinneligFakturaSerie.faktura shouldHaveSize 2
        opprinneligFakturaSerie.faktura.forEach {
            it.status = FakturaStatus.BESTILT
        }

        val nyFakturaSerie = lagFakturaserieForEndring(
            intervall = FakturaserieIntervall.KVARTAL,
            perioder = listOf(
                FakturaseriePeriode.forTest {
                    månedspris = 2000
                    fra = "2024-01-01"
                    til = "2024-06-30"
                    beskrivelse = "Inntekt"
                }
            ),
            opprinneligFakturaserie = opprinneligFakturaSerie
        )

        // Kun avregningsfakturaer - ingen nye fakturaer
        nyFakturaSerie.faktura shouldHaveSize 2
    }

    @Test
    fun `Skal kaste feil hvis ingen avregningsfaktura eller periode`() {
        unleash.apply { enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER) }
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.of(2025, 8, 27)


        val opprinneligFakturaSerie = lagFakturaserie(
            intervall = FakturaserieIntervall.KVARTAL,
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2025, 12, 31),
                    beskrivelse = "Inntekt: 10000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            )
        )

        val exception = shouldThrow<IllegalStateException> {
            lagFakturaserieForEndring(
                intervall = FakturaserieIntervall.KVARTAL,
                perioder = emptyList(),
                opprinneligFakturaserie = opprinneligFakturaSerie
            )
        }
        exception.message shouldContain "Kan ikke opprette fakturaserie med tomme perioder og ingen avregningsfakturaer"

    }


    @Test
    fun `Fakturaserie skal kun avregne inneværende og fremtidige perioder ved tom periodeinput`() {
        unleash.apply { enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER) }
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns LocalDate.of(2025, 8, 27)


        val opprinneligFakturaSerie = lagFakturaserie(
            intervall = FakturaserieIntervall.KVARTAL,
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2025, 12, 31),
                    beskrivelse = "Inntekt: 10000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            )
        )

        opprinneligFakturaSerie.faktura.forEach {
            it.status = FakturaStatus.BESTILT
        }

        val nyFakturaSerie = lagFakturaserieForEndring(
            intervall = FakturaserieIntervall.KVARTAL,
            perioder = emptyList(),
            opprinneligFakturaserie = opprinneligFakturaSerie
        )

        ForventetFakturering(nyFakturaSerie.faktura).shouldBeEqualToComparingFields(
            ForventetFakturering(
                2,
                listOf(
                    FakturaMedLinjer(
                        fra = "2025-01-01", til = "2025-09-30",
                        listOf(
                            Linje(
                                "2025-01-01", "2025-09-30", "-90000.00",
                                "Nytt beløp: 0,00 - tidligere beløp: 90000,00"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2025-10-01", til = "2025-12-31",
                        listOf(
                            Linje(
                                "2025-10-01", "2025-12-31", "-30000.00",
                                "Nytt beløp: 0,00 - tidligere beløp: 30000,00"
                            ),
                        )
                    )
                )
            )
        )
    }

    private fun lagFakturaserie(
        dagensDato: LocalDate = LocalDate.now(),
        intervall: FakturaserieIntervall = FakturaserieIntervall.MANEDLIG,
        perioder: List<FakturaseriePeriode> = listOf()
    ): Fakturaserie {
        val fakturaMapper = FakturaGeneratorForTest(dagensDato, unleash = unleash)
        return FakturaserieGenerator(fakturaGenerator = fakturaMapper, AvregningBehandler(AvregningsfakturaGenerator()), unleash).lagFakturaserie(
            FakturaserieDto(
                fakturaserieReferanse = ULID.randomULID(),
                fodselsnummer = "30056928150",
                fullmektig = Fullmektig(
                    fodselsnummer = null,
                    organisasjonsnummer = "999999999",
                ),
                referanseBruker = "Vedtak om medlemskap datert 19-01-2023",
                referanseNAV = "Medlemskap og avgift",
                fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
                intervall = intervall,
                perioder = perioder
            )
        )
    }

    private fun lagFakturaserieForEndring(
        dagensDato: LocalDate = LocalDate.now(),
        intervall: FakturaserieIntervall = FakturaserieIntervall.MANEDLIG,
        perioder: List<FakturaseriePeriode> = listOf(),
        opprinneligFakturaserie: Fakturaserie
    ): Fakturaserie {
        val fakturaMapper = FakturaGeneratorForTest(dagensDato, unleash = unleash)
        return FakturaserieGenerator(
            fakturaGenerator = fakturaMapper,
            AvregningBehandler(AvregningsfakturaGenerator()),
            unleash
        ).lagFakturaserieForEndring(
            FakturaserieDto(
                fakturaserieReferanse = ULID.randomULID(),
                fodselsnummer = "30056928150",
                fullmektig = Fullmektig(
                    fodselsnummer = null,
                    organisasjonsnummer = "999999999",
                ),
                referanseBruker = "Vedtak om medlemskap datert 19-01-2023",
                referanseNAV = "Medlemskap og avgift",
                fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
                intervall = intervall,
                perioder = perioder
            ),
            opprinneligFakturaserie
        )
    }


    class FakturaGeneratorForTest(private val dagensDato: LocalDate, unleash: FakeUnleash) :
        FakturaGenerator(FakturaLinjeGenerator(), unleash = unleash, 0) {
        override fun dagensDato(): LocalDate = dagensDato
    }

    data class ForventetFakturering(
        val antallFakturaer: Int,
        val fakturaMedLinjer: List<FakturaMedLinjer>

    ) {
        constructor(fakturaListe: List<Faktura>) :
            this(
                fakturaListe.size,
                fakturaListe.map { f ->
                    FakturaMedLinjer(
                        f.getPeriodeFra(),
                        f.getPeriodeTil(),
                        f.fakturaLinje.map { fl ->
                            Linje(
                                fl.periodeFra,
                                fl.periodeTil,
                                fl.belop,
                                fl.beskrivelse
                            )
                        }
                    )
                })

        override fun toString() = "antall=$antallFakturaer fakturaListe=$fakturaMedLinjer\n"

        fun toTestCode(): String =
            "FakturaData(\n" +
                "  $antallFakturaer,\n  listOf(\n" +
                fakturaMedLinjer.joinToString("\n") { it.toTestCode() } +
                "\n )" +
                "\n)"

    }

    data class FakturaMedLinjer(
        val fra: LocalDate,
        val til: LocalDate,
        val fakturaLinjer: List<Linje>
    ) {
        constructor(fra: String, til: String, fakturaLinjer: List<Linje>) :
            this(LocalDate.parse(fra), LocalDate.parse(til), fakturaLinjer)

        override fun toString() = "\n" +
            "  fra:$fra, til:$til fakturaLinjer:$fakturaLinjer\n"

        fun toTestCode(): String = "    FakturaMedLinjer(\n" +
            "      fra = \"$fra\", til = \"$til\",\n" +
            "      listOf(\n" +
            "${fakturaLinjer.joinToString("\n") { it.toTestCode() }}\n" +
            "      )\n" +
            "  ),"
    }

    data class Linje(
        val fra: LocalDate,
        val til: LocalDate,
        val beløp: BigDecimal,
        val beskrivelse: String,
    ) {

        companion object {
            val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        }

        constructor(fra: String, til: String, beløp: String, beskrivelse: String)
            : this(
            LocalDate.parse(fra),
            LocalDate.parse(til),
            BigDecimal(beløp),
            "Periode: ${LocalDate.parse(fra).format(FORMATTER)} - ${
                LocalDate.parse(til).format(FORMATTER)
            }\n$beskrivelse"
        )

        override fun toString() = "\n    fra=$fra, til:$til, beløp:$beløp, $beskrivelse"
        fun toTestCode(): String = "           Linje(\n" +
            "             \"$fra\", \"$til\", $beløp,\n " +
            "             \"$beskrivelse\"\n" +
            "            ),"
    }

    @Nested
    inner class LocalDateRangeSubstraction {
        @Test
        fun `substract a period`() {
            val substracted = LocalDateRange.of(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 10)
            ).substract(
                LocalDateRange.of(
                    LocalDate.of(2024, 1, 3),
                    LocalDate.of(2024, 2, 12)
                )
            )

            substracted shouldBe listOf(
                LocalDateRange.of(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 3)
                )
            )
        }

        @Test
        fun `substract a period in the middle`() {
            val substracted = LocalDateRange.of(
                LocalDate.of(2023, 12, 16),
                LocalDate.of(2024, 1, 15)
            ).substract(
                LocalDateRange.of(
                    LocalDate.of(2023, 12, 31),
                    LocalDate.of(2024, 1, 13)
                )
            )

            substracted.shouldContainExactlyInAnyOrder(
                LocalDateRange.of(LocalDate.of(2023, 12, 16), LocalDate.of(2023, 12, 31)),
                LocalDateRange.of(LocalDate.of(2024, 1, 13), LocalDate.of(2024, 1, 15))
            )
        }

        @Test
        fun `substract identical period returns empty`() {
            val substracted = LocalDateRange.ofClosed(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 3, 31)
            ).substract(
                LocalDateRange.ofClosed(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 3, 31)
                )
            )

            substracted.shouldBeEmpty()
        }

        @Test
        fun `substract non-overlapping period returns original`() {
            val original = LocalDateRange.ofClosed(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 31)
            )
            val substracted = original.substract(
                LocalDateRange.ofClosed(
                    LocalDate.of(2024, 3, 1),
                    LocalDate.of(2024, 3, 31)
                )
            )

            substracted shouldBe listOf(original)
        }

        @Test
        fun `substract start of closed range leaves end - MEL-15767 off-by-one`() {
            // Reproduserer MEL-15767: ofClosed(01.01, 03.15) minus ofClosed(01.01, 02.29)
            // Skal gi mars 1-15 (ikke mars 2-15)
            val substracted = LocalDateRange.ofClosed(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 3, 15)
            ).substract(
                LocalDateRange.ofClosed(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 2, 29)
                )
            )

            substracted shouldBe listOf(
                // ofClosed(01.01, 02.29) stored as [01.01, 03.01), so remainder starts at 03.01
                LocalDateRange.of(
                    LocalDate.of(2024, 3, 1),
                    LocalDate.of(2024, 3, 16)
                )
            )
        }

        @Test
        fun `substract middle of closed range leaves both ends`() {
            val substracted = LocalDateRange.ofClosed(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 6, 30)
            ).substract(
                LocalDateRange.ofClosed(
                    LocalDate.of(2024, 2, 1),
                    LocalDate.of(2024, 3, 31)
                )
            )

            substracted.shouldContainExactlyInAnyOrder(
                LocalDateRange.of(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1)),
                LocalDateRange.of(LocalDate.of(2024, 4, 1), LocalDate.of(2024, 7, 1))
            )
        }
    }
}
