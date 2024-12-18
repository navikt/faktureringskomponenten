package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakturaGeneratorParameterizedTest {

    private val fakturaLinjeGenerator = FakturaLinjeGenerator()
    private val unleash = FakeUnleash().also {
        it.enableAllExcept("melosys.faktureringskomponent.send_faktura_instant")
    }
    private val generator = FakturaGenerator(fakturaLinjeGenerator, unleash, 0)

    @AfterEach
    fun tearDown() {
        unmockkStatic(LocalDate::class)
    }

    data class DateTestCase(
        val description: String,
        val dagensDato: LocalDate,
        val perioder: List<FakturaseriePeriode>,
        val intervall: FakturaserieIntervall,
        val expectedDatoBestilt: List<LocalDate>
    )

    data class PeriodTestCase(
        val description: String,
        val dagensDato: LocalDate,
        val perioder: List<FakturaseriePeriode>,
        val intervall: FakturaserieIntervall,
        val expectedPerioder: List<Pair<LocalDate, LocalDate>>
    )

    data class BelopTestCase(
        val description: String,
        val dagensDato: LocalDate,
        val perioder: List<FakturaseriePeriode>,
        val intervall: FakturaserieIntervall,
        val expectedBelopPerFaktura: List<ExpectedBelop>
    )

    data class ExpectedBelop(
        val periodeFra: LocalDate,
        val periodeTil: LocalDate,
        val belop: BigDecimal
    )

    @ParameterizedTest(name = "{index} {argumentSetName}")
    @MethodSource("belopTestData")
    @DisplayName("Test beløpsberegning for delvise perioder")
    fun `test beløpsberegning for delvise perioder`(testCase: BelopTestCase) {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns testCase.dagensDato

        val periodisering = FakturaIntervallPeriodisering.genererPeriodisering(
            testCase.perioder.minOf { it.startDato },
            testCase.perioder.maxOf { it.sluttDato },
            testCase.intervall
        )

        val fakturaer = generator.lagFakturaerFor(periodisering, testCase.perioder, testCase.intervall)

        return fakturaer.forEachIndexed { index, faktura ->
            val expected = testCase.expectedBelopPerFaktura[index]
            faktura.getPeriodeFra() shouldBe expected.periodeFra
            faktura.getPeriodeTil() shouldBe expected.periodeTil
            faktura.fakturaLinje.sumOf { it.belop } shouldBe expected.belop
        }
    }


    @ParameterizedTest(name = "{index} {argumentSetName}")
    @MethodSource("datoBestiltTestData")
    @DisplayName("Test ulike scenarioer for dato bestilt")
    fun `test dato bestilt scenarios`(testCase: DateTestCase) {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns testCase.dagensDato

        val periodisering = FakturaIntervallPeriodisering.genererPeriodisering(
            testCase.perioder.minOf { it.startDato },
            testCase.perioder.maxOf { it.sluttDato },
            testCase.intervall
        )

        val fakturaer = generator.lagFakturaerFor(periodisering, testCase.perioder, testCase.intervall)

        fakturaer.shouldHaveSize(testCase.expectedDatoBestilt.size)
        fakturaer.map { it.datoBestilt }.shouldContainExactlyInAnyOrder(testCase.expectedDatoBestilt)
    }

    @ParameterizedTest(name = "{index} {argumentSetName}")
    @MethodSource("periodTestData")
    @DisplayName("Test ulike scenarioer for periodebehandling")
    fun `test period handling scenarios`(testCase: PeriodTestCase) {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns testCase.dagensDato

        val periodisering = FakturaIntervallPeriodisering.genererPeriodisering(
            testCase.perioder.minOf { it.startDato },
            testCase.perioder.maxOf { it.sluttDato },
            testCase.intervall
        )

        val fakturaer = generator.lagFakturaerFor(periodisering, testCase.perioder, testCase.intervall)

        fakturaer.shouldHaveSize(testCase.expectedPerioder.size)
        fakturaer.map { it.getPeriodeFra() to it.getPeriodeTil() }
            .shouldContainExactlyInAnyOrder(testCase.expectedPerioder)
    }

    private fun datoBestiltTestData() = listOf(
        // Test case: Kvartalsvis fakturering, fremtidig periode
        DateTestCase(
            description = "Kvartalsvis fakturering for fremtidig periode",
            dagensDato = LocalDate.of(2024, 1, 15),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("1000"),
                    startDato = LocalDate.of(2024, 4, 1),
                    sluttDato = LocalDate.of(2024, 12, 31),
                    beskrivelse = "Test periode 1"
                )
            ),
            intervall = FakturaserieIntervall.KVARTAL,
            expectedDatoBestilt = listOf(
                LocalDate.of(2024, 3, 19),
                LocalDate.of(2024, 6, 19),
                LocalDate.of(2024, 9, 19)
            )
        ),

        // Test case: Månedsvis fakturering, blandet historisk og fremtidig
        DateTestCase(
            description = "Månedsvis fakturering med historiske og fremtidige perioder",
            dagensDato = LocalDate.of(2024, 1, 15),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("1000"),
                    startDato = LocalDate.of(2023, 12, 1),
                    sluttDato = LocalDate.of(2024, 3, 31),
                    beskrivelse = "Test periode 2"
                )
            ),
            intervall = FakturaserieIntervall.MANEDLIG,
            expectedDatoBestilt = listOf(
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 1, 19),
                LocalDate.of(2024, 2, 19)
            )
        ),

        // Test case: Kvartalsvis på årsskifte
        DateTestCase(
            description = "Kvartalsvis fakturering over årsskifte",
            dagensDato = LocalDate.of(2023, 12, 20),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("1000"),
                    startDato = LocalDate.of(2023, 12, 1),
                    sluttDato = LocalDate.of(2024, 3, 31),
                    beskrivelse = "Test periode 3"
                )
            ),
            intervall = FakturaserieIntervall.KVARTAL,
            expectedDatoBestilt = listOf(
                LocalDate.of(2023, 12, 20),
                LocalDate.of(2023, 12, 20)
            )
        )
    ).map { argumentSet(it.description, it) }

    private fun periodTestData() = listOf(
        // Test case: Overlappende perioder
        PeriodTestCase(
            description = "Overlappende perioder",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("1000"),
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2024, 3, 31),
                    beskrivelse = "Periode 1"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("2000"),
                    startDato = LocalDate.of(2024, 3, 15),
                    sluttDato = LocalDate.of(2024, 6, 30),
                    beskrivelse = "Periode 2"
                )
            ),
            intervall = FakturaserieIntervall.KVARTAL,
            expectedPerioder = listOf(
                LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 3, 31),
                LocalDate.of(2024, 4, 1) to LocalDate.of(2024, 6, 30)
            )
        ),

        // Test case: Perioder med opphold
        PeriodTestCase(
            description = "Perioder med opphold",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("1000"),
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2024, 3, 31),
                    beskrivelse = "Periode 1"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("1000"),
                    startDato = LocalDate.of(2024, 7, 1),
                    sluttDato = LocalDate.of(2024, 9, 30),
                    beskrivelse = "Periode 2"
                )
            ),
            intervall = FakturaserieIntervall.KVARTAL,
            expectedPerioder = listOf(
                LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 3, 31),
                LocalDate.of(2024, 7, 1) to LocalDate.of(2024, 9, 30)
            )
        ),

        // Test case: Delvis måned
        PeriodTestCase(
            description = "Periode starter midt i måneden",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("1000"),
                    startDato = LocalDate.of(2024, 1, 15),
                    sluttDato = LocalDate.of(2024, 3, 31),
                    beskrivelse = "Delvis måned"
                )
            ),
            intervall = FakturaserieIntervall.MANEDLIG,
            expectedPerioder = listOf(
                LocalDate.of(2024, 1, 15) to LocalDate.of(2024, 1, 31),
                LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 2, 29),
                LocalDate.of(2024, 3, 1) to LocalDate.of(2024, 3, 31)
            )
        ),

        // Test case: Skuddårhåndtering
        PeriodTestCase(
            description = "Skuddår håndtering 2024",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("1000"),
                    startDato = LocalDate.of(2024, 2, 1),
                    sluttDato = LocalDate.of(2024, 3, 15),
                    beskrivelse = "Skuddår periode"
                )
            ),
            intervall = FakturaserieIntervall.MANEDLIG,
            expectedPerioder = listOf(
                LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 2, 29),
                LocalDate.of(2024, 3, 1) to LocalDate.of(2024, 3, 15)
            )
        ),

        PeriodTestCase(
            description = "Årsskifte med delvis kvartaler",
            dagensDato = LocalDate.of(2023, 12, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("1000"),
                    startDato = LocalDate.of(2023, 11, 15),
                    sluttDato = LocalDate.of(2024, 2, 15),
                    beskrivelse = "Årsskifte periode"
                )
            ),
            intervall = FakturaserieIntervall.KVARTAL,
            expectedPerioder = listOf(
                LocalDate.of(2023, 11, 15) to LocalDate.of(2023, 12, 31),
                LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 2, 15)
            )
        ),

        PeriodTestCase(
            description = "Tre overlappende perioder med ulike priser",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("1000"),
                    startDato = LocalDate.of(2024, 1, 1),
                    sluttDato = LocalDate.of(2024, 4, 30),
                    beskrivelse = "Periode 1"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("2000"),
                    startDato = LocalDate.of(2024, 3, 15),
                    sluttDato = LocalDate.of(2024, 7, 15),
                    beskrivelse = "Periode 2"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("3000"),
                    startDato = LocalDate.of(2024, 6, 1),
                    sluttDato = LocalDate.of(2024, 9, 30),
                    beskrivelse = "Periode 3"
                )
            ),
            intervall = FakturaserieIntervall.KVARTAL,
            expectedPerioder = listOf(
                LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 3, 31),
                LocalDate.of(2024, 4, 1) to LocalDate.of(2024, 6, 30),
                LocalDate.of(2024, 7, 1) to LocalDate.of(2024, 9, 30)
            )
        ),

        // Test case for periode som starter siste dag i måneden
        PeriodTestCase(
            description = "Periode starter siste dag i måneder med ulik lengde",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("30000"),  // 1000 kr per dag
                    startDato = LocalDate.of(2024, 1, 31),     // 31 dager
                    sluttDato = LocalDate.of(2024, 4, 30),     // 30 dager
                    beskrivelse = "Test periode siste dag"
                )
            ),
            intervall = FakturaserieIntervall.MANEDLIG,
            expectedPerioder = listOf(
                LocalDate.of(2024, 1, 31) to LocalDate.of(2024, 1, 31),  // 1 dag i januar
                LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 2, 29),   // hele februar (skuddår)
                LocalDate.of(2024, 3, 1) to LocalDate.of(2024, 3, 31),   // hele mars
                LocalDate.of(2024, 4, 1) to LocalDate.of(2024, 4, 30)    // hele april
            )
        ),
    ).map { argumentSet(it.description, it) }

    private fun belopTestData() = listOf(
        // Test case for avrunding av delvise måneder
        BelopTestCase(
            description = "Avrunding for delvis måned med 31 dager",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("31000"),
                    startDato = LocalDate.of(2024, 1, 15),
                    sluttDato = LocalDate.of(2024, 1, 31),
                    beskrivelse = "Test delvis januar"
                )
            ),
            intervall = FakturaserieIntervall.MANEDLIG,
            expectedBelopPerFaktura = listOf(
                ExpectedBelop(
                    LocalDate.of(2024, 1, 15),
                    LocalDate.of(2024, 1, 31),
                    BigDecimal("17050.00")  // 31000 * (17/31) ≈ 17050.00 der 17/31 ≈ 0.55
                )
            )
        ),

        BelopTestCase(
            description = "Avrunding for delvis måned med overgang mellom måneder",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("30000"),
                    startDato = LocalDate.of(2024, 1, 20),
                    sluttDato = LocalDate.of(2024, 1, 31),
                    beskrivelse = "Test periode over månedsskifte"
                )
            ),
            intervall = FakturaserieIntervall.MANEDLIG,
            expectedBelopPerFaktura = listOf(
                ExpectedBelop(
                    LocalDate.of(2024, 1, 20),
                    LocalDate.of(2024, 1, 31),
                    BigDecimal("11700.00")  // 30000 * (12/31) ≈ 11700.00 der 12/31 ≈ 0.39
                )
            )
        ),

        BelopTestCase(
            description = "Avrunding for delvis måned i skuddår (februar)",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("29000"),
                    startDato = LocalDate.of(2024, 2, 15),
                    sluttDato = LocalDate.of(2024, 2, 29),
                    beskrivelse = "Test delvis februar i skuddår"
                )
            ),
            intervall = FakturaserieIntervall.MANEDLIG,
            expectedBelopPerFaktura = listOf(
                ExpectedBelop(
                    LocalDate.of(2024, 2, 15),
                    LocalDate.of(2024, 2, 29),
                    BigDecimal("15080.00")  // 29000 * (15/29) ≈ 15080.00 der 15/29 ≈ 0.51
                )
            )
        )
    ).map { argumentSet(it.description, it) }
}
