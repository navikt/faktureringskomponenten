package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import java.util.stream.Stream

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
        val expectedFakturaCount: Int,
        val expectedDatoBestilt: List<LocalDate>
    )

    data class PeriodTestCase(
        val description: String,
        val dagensDato: LocalDate,
        val perioder: List<FakturaseriePeriode>,
        val intervall: FakturaserieIntervall,
        val expectedFakturaCount: Int,
        val expectedPeriods: List<Pair<LocalDate, LocalDate>>
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

    @ParameterizedTest(name = "Beløpsberegning: {0}")
    @MethodSource("belopTestData")
    @DisplayName("Test beløpsberegning for delvise perioder")
    fun `test belop calculation for partial periods`(testCase: BelopTestCase) {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns testCase.dagensDato

        val periodisering = FakturaserieGenerator.genererPeriodisering(
            testCase.perioder.minOf { it.startDato },
            testCase.perioder.maxOf { it.sluttDato },
            testCase.intervall
        )

        val fakturaer = generator.lagFakturaerFor(periodisering, testCase.perioder, testCase.intervall)

        fakturaer.forEachIndexed { index, faktura ->
            val expected = testCase.expectedBelopPerFaktura[index]
            faktura.getPeriodeFra() shouldBe expected.periodeFra
            faktura.getPeriodeTil() shouldBe expected.periodeTil
            faktura.fakturaLinje.sumOf { it.belop } shouldBe expected.belop
        }
    }


    @ParameterizedTest(name = "Dato bestilt: {0}")
    @MethodSource("datoBestiltTestData")
    @DisplayName("Test ulike scenarioer for dato bestilt")
    fun `test dato bestilt scenarios`(testCase: DateTestCase) {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns testCase.dagensDato

        val periodisering = FakturaserieGenerator.genererPeriodisering(
            testCase.perioder.minOf { it.startDato },
            testCase.perioder.maxOf { it.sluttDato },
            testCase.intervall
        )

        val fakturaer = generator.lagFakturaerFor(periodisering, testCase.perioder, testCase.intervall)

        fakturaer.shouldHaveSize(testCase.expectedFakturaCount)
        fakturaer.map { it.datoBestilt }.shouldContainExactlyInAnyOrder(testCase.expectedDatoBestilt)
    }

    @ParameterizedTest(name = "Periodehåndtering: {0}")
    @MethodSource("periodTestData")
    @DisplayName("Test ulike scenarioer for periodebehandling")
    fun `test period handling scenarios`(testCase: PeriodTestCase) {
        mockkStatic(LocalDate::class)
        every { LocalDate.now() } returns testCase.dagensDato

        val periodisering = FakturaserieGenerator.genererPeriodisering(
            testCase.perioder.minOf { it.startDato },
            testCase.perioder.maxOf { it.sluttDato },
            testCase.intervall
        )

        val fakturaer = generator.lagFakturaerFor(periodisering, testCase.perioder, testCase.intervall)

        fakturaer.shouldHaveSize(testCase.expectedFakturaCount)
        fakturaer.map { it.getPeriodeFra() to it.getPeriodeTil() }
            .shouldContainExactlyInAnyOrder(testCase.expectedPeriods)
    }

    private fun datoBestiltTestData(): Stream<Arguments> = Stream.of(
        // Test case: Kvartalsvis fakturering, fremtidig periode
        Arguments.of(
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
                expectedFakturaCount = 3,
                expectedDatoBestilt = listOf(
                    LocalDate.of(2024, 3, 19),
                    LocalDate.of(2024, 6, 19),
                    LocalDate.of(2024, 9, 19)
                )
            )
        ),

        // Test case: Månedsvis fakturering, blandet historisk og fremtidig
        Arguments.of(
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
                expectedFakturaCount = 4,
                expectedDatoBestilt = listOf(
                    LocalDate.of(2024, 1, 15),
                    LocalDate.of(2024, 1, 15),
                    LocalDate.of(2024, 1, 19),
                    LocalDate.of(2024, 2, 19)
                )
            )
        ),

        // Test case: Kvartalsvis på årsskifte
        Arguments.of(
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
                expectedFakturaCount = 2,
                expectedDatoBestilt = listOf(
                    LocalDate.of(2023, 12, 20),
                    LocalDate.of(2023, 12, 20)
                )
            )
        )
    )

    private fun periodTestData(): Stream<Arguments> = Stream.of(
        // Test case: Overlappende perioder
        Arguments.of(
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
                expectedFakturaCount = 2,
                expectedPeriods = listOf(
                    LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 3, 31),
                    LocalDate.of(2024, 4, 1) to LocalDate.of(2024, 6, 30)
                )
            )
        ),

        // Test case: Perioder med opphold
        Arguments.of(
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
                expectedFakturaCount = 2,
                expectedPeriods = listOf(
                    LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 3, 31),
                    LocalDate.of(2024, 7, 1) to LocalDate.of(2024, 9, 30)
                )
            )
        ),

        // Test case: Delvis måned
        Arguments.of(
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
                expectedFakturaCount = 3,
                expectedPeriods = listOf(
                    LocalDate.of(2024, 1, 15) to LocalDate.of(2024, 1, 31),
                    LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 2, 29),
                    LocalDate.of(2024, 3, 1) to LocalDate.of(2024, 3, 31)
                )
            )
        ),

        // Test case: Skuddårhåndtering
        Arguments.of(
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
                expectedFakturaCount = 2,
                expectedPeriods = listOf(
                    LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 2, 29),
                    LocalDate.of(2024, 3, 1) to LocalDate.of(2024, 3, 15)
                )
            )
        ),

        Arguments.of(PeriodTestCase(
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
            expectedFakturaCount = 2,
            expectedPeriods = listOf(
                LocalDate.of(2023, 11, 15) to LocalDate.of(2023, 12, 31),
                LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 2, 15)
            )
        )),

        Arguments.of(
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
                expectedFakturaCount = 3,
                expectedPeriods = listOf(
                    LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 3, 31),
                    LocalDate.of(2024, 4, 1) to LocalDate.of(2024, 6, 30),
                    LocalDate.of(2024, 7, 1) to LocalDate.of(2024, 9, 30)
                )
            )
        ),



        // Test case for periode som starter siste dag i måneden
        Arguments.of(PeriodTestCase(
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
            expectedFakturaCount = 4,
            expectedPeriods = listOf(
                LocalDate.of(2024, 1, 31) to LocalDate.of(2024, 1, 31),  // 1 dag i januar
                LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 2, 29),   // hele februar (skuddår)
                LocalDate.of(2024, 3, 1) to LocalDate.of(2024, 3, 31),   // hele mars
                LocalDate.of(2024, 4, 1) to LocalDate.of(2024, 4, 30)    // hele april
            )
        )),
    )

    private fun belopTestData(): Stream<Arguments> = Stream.of(
        // Test case for avrunding av delvise måneder
        Arguments.of(BelopTestCase(
            description = "Avrunding for delvis måned med 31 dager",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("31000"),  // 1000 kr per dag
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
                    BigDecimal("17000.00")  // 17 dager * 1000 kr
                )
            )
        )),

        Arguments.of(BelopTestCase(
            description = "Avrunding for delvis måned med overgang mellom måneder",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("30000"),  // Cirka 1000 kr per dag
                    startDato = LocalDate.of(2024, 1, 20),
                    sluttDato = LocalDate.of(2024, 2, 10),
                    beskrivelse = "Test periode over månedsskifte"
                )
            ),
            intervall = FakturaserieIntervall.MANEDLIG,
            expectedBelopPerFaktura = listOf(
                ExpectedBelop(
                    LocalDate.of(2024, 1, 20),
                    LocalDate.of(2024, 1, 31),
                    BigDecimal("12000.00")  // 12 dager i januar
                ),
                ExpectedBelop(
                    LocalDate.of(2024, 2, 1),
                    LocalDate.of(2024, 2, 10),
                    BigDecimal("10000.00")  // 10 dager i februar
                )
            )
        )),

        Arguments.of(BelopTestCase(
            description = "Avrunding for delvis måned i skuddår (februar)",
            dagensDato = LocalDate.of(2024, 1, 1),
            perioder = listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal("29000"),  // 1000 kr per dag i februar 2024
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
                    BigDecimal("15000.00")  // 15 dager
                )
            )
        ))
    )

    private fun Faktura.getPeriodeFra(): LocalDate = fakturaLinje.minOfOrNull { it.periodeFra }
        ?: throw IllegalStateException("Faktura uten fakturalinjer")

    private fun Faktura.getPeriodeTil(): LocalDate = fakturaLinje.maxOfOrNull { it.periodeTil }
        ?: throw IllegalStateException("Faktura uten fakturalinjer")
}
