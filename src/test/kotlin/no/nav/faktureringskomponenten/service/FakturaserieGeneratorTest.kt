package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
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
        unleash.apply { disable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER) }
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

    private fun data() = listOf(
        arguments(
            "Går 1 dag inn i neste måned",
            LocalDate.of(2023, 1, 13),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 11, 1),
                    sluttDato = LocalDate.of(2022, 12, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            ForventetFakturering(
                1,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-11-01", til = "2022-12-01",
                        listOf(
                            Linje(
                                "2022-12-01", "2022-12-01", "764.10",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2022-11-01", "2022-11-30", "25470.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            )
                        )
                    )
                )
            )
        ),

        arguments(
            "Medlemskap starter i 2022, fortsetter i 2023, egen faktura for perioden før årsovergang",
            LocalDate.of(2023, 1, 26),
            FakturaserieIntervall.KVARTAL,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2022, 6, 1),
                    sluttDato = LocalDate.of(2023, 1, 24),
                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2023, 1, 25),
                    sluttDato = LocalDate.of(2023, 6, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            ForventetFakturering(
                3,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-06-01", til = "2022-12-31",
                        listOf(
                            Linje(
                                "2022-10-01", "2022-12-31", "30000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-07-01", "2022-09-30", "30000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-06-01", "2022-06-30", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-01-01", til = "2023-03-31",
                        listOf(
                            Linje(
                                "2023-01-25", "2023-03-31", "22300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-24", "7700.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            )
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-04-01", til = "2023-06-01",
                        listOf(
                            Linje(
                                "2023-04-01", "2023-06-01", "20300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                )
            )
        ),

        arguments(
            "Datoer i samme måned",
            LocalDate.of(2023, 1, 1),
            FakturaserieIntervall.KVARTAL,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(1000),
                    startDato = LocalDate.of(2023, 1, 15),
                    sluttDato = LocalDate.of(2023, 1, 30),
                    beskrivelse = "periode - 1"
                )
            ),
            ForventetFakturering(
                1,
                listOf(
                    FakturaMedLinjer(
                        fra = "2023-01-15", til = "2023-01-30",
                        listOf(
                            Linje(
                                "2023-01-15", "2023-01-30", "520.00",
                                "periode - 1"
                            )
                        )
                    )
                )
            )
        ),


        arguments(
            "Slutt dato er før dagens dato",
            LocalDate.of(2023, 4, 1),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2023, 1, 1),
                    sluttDato = LocalDate.of(2023, 2, 1),
                    beskrivelse = "periode - 1"
                )
            ),
            ForventetFakturering(
                1,
                listOf(
                    FakturaMedLinjer(
                        fra = "2023-01-01", til = "2023-02-01",
                        listOf(
                            Linje(
                                "2023-02-01", "2023-02-01", "400.00",
                                "periode - 1"
                            ),
                            Linje(
                                "2023-01-01", til = "2023-01-31", "10000.00",
                                "periode - 1"
                            ),
                        )
                    )
                )
            )
        ),

        arguments(
            "Dagens dato er lik slutt dato",
            LocalDate.of(2022, 12, 31),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 11, 1),
                    sluttDato = LocalDate.of(2022, 12, 31),
                    beskrivelse = "periode 1"
                )
            ),
            ForventetFakturering(
                1,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-11-01", til = "2022-12-31",
                        listOf(
                            Linje(
                                "2022-12-01", "2022-12-31", "25470.00",
                                "periode 1"
                            ),
                            Linje(
                                "2022-11-01", "2022-11-30", "25470.00",
                                "periode 1"
                            )
                        )
                    )
                )
            )
        ),

        arguments(
            "Før og etter dagens dato",
            LocalDate.of(2022, 4, 13),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 3, 1),
                    sluttDato = LocalDate.of(2022, 5, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            ForventetFakturering(
                2,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-03-01", til = "2022-04-30",
                        listOf(
                            Linje(
                                "2022-04-01", "2022-04-30", "25470.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2022-03-01", "2022-03-31", "25470.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            )
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2022-05-01", til = "2022-05-01",
                        listOf(
                            Linje(
                                "2022-05-01", "2022-05-01", "764.10",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            )
                        )
                    )
                )
            )
        ),

        arguments(
            "2 faktura perioder - kun 1 dag i siste måned",
            LocalDate.of(2023, 1, 23),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 12, 1),
                    sluttDato = LocalDate.of(2023, 1, 22),
                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2023, 1, 23),
                    sluttDato = LocalDate.of(2023, 2, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            ForventetFakturering(
                3,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-12-01", til = "2022-12-31",
                        listOf(
                            Linje(
                                "2022-12-01", "2022-12-31", "25470.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-01-01", til = "2023-01-31",
                        listOf(
                            Linje(
                                "2023-01-23", "2023-01-31", "7386.30",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-22", "18083.70",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            )
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-02-01", til = "2023-02-01",
                        listOf(
                            Linje(
                                "2023-02-01", "2023-02-01", "1018.80",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                )
            )
        ),

        arguments(
            "2 perioder - lag 7 fakturaer med linjer",
            LocalDate.of(2023, 1, 26),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2022, 6, 1),
                    sluttDato = LocalDate.of(2023, 1, 24),
                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2023, 1, 25),
                    sluttDato = LocalDate.of(2023, 6, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            ForventetFakturering(
                7,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-06-01", til = "2022-12-31",
                        listOf(
                            Linje(
                                "2022-12-01", "2022-12-31", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-11-01", "2022-11-30", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-10-01", "2022-10-31", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-09-01", "2022-09-30", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-08-01", "2022-08-31", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-07-01", "2022-07-31", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-06-01", "2022-06-30", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            )
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-01-01", til = "2023-01-31",
                        listOf(
                            Linje(
                                "2023-01-25", "2023-01-31", "2300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-24", "7700.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            )
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-02-01", til = "2023-02-28",
                        listOf(
                            Linje(
                                "2023-02-01", "2023-02-28", "10000.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-03-01", til = "2023-03-31",
                        listOf(
                            Linje(
                                "2023-03-01", "2023-03-31", "10000.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-04-01", til = "2023-04-30",
                        listOf(
                            Linje(
                                "2023-04-01", "2023-04-30", "10000.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-05-01", til = "2023-05-31",
                        listOf(
                            Linje(
                                "2023-05-01", "2023-05-31", "10000.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-06-01", til = "2023-06-01",
                        listOf(
                            Linje(
                                "2023-06-01", "2023-06-01", "300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                )
            )
        ),

        arguments(
            "2 perioder - lag 3 fakturaer med linjer",
            LocalDate.of(2023, 1, 26),
            FakturaserieIntervall.KVARTAL,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2022, 6, 1),
                    sluttDato = LocalDate.of(2023, 1, 24),
                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2023, 1, 25),
                    sluttDato = LocalDate.of(2023, 6, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            ForventetFakturering(
                3,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-06-01", til = "2022-12-31",
                        listOf(
                            Linje(
                                "2022-10-01", "2022-12-31", "30000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-07-01", "2022-09-30", "30000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-06-01", "2022-06-30", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            )
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-01-01", til = "2023-03-31",
                        listOf(
                            Linje(
                                "2023-01-25", "2023-03-31", "22300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-24", "7700.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            )
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-04-01", til = "2023-06-01",
                        listOf(
                            Linje(
                                "2023-04-01", "2023-06-01", "20300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                )
            )
        )
    )

    @Test
    fun `Finnes eksisterende fakturaserie fra tidligere kalenderår - disse skal avregnes når toggle er av`() {
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
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(5000),
                    startDato = LocalDate.of(2025, 1, 1),
                    sluttDato = LocalDate.of(2025, 12, 31),
                    beskrivelse = "Inntekt: 5000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
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
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(5000),
                    startDato = LocalDate.of(2025, 1, 1),
                    sluttDato = LocalDate.of(2025, 12, 31),
                    beskrivelse = "Inntekt: 5000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
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
                    LocalDate.of(2024, 1, 2)
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
                LocalDateRange.of(LocalDate.of(2023, 12, 16), LocalDate.of(2023, 12, 30)),
                LocalDateRange.of(LocalDate.of(2024, 1, 14), LocalDate.of(2024, 1, 15))
            )
        }
    }
}
