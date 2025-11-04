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
import no.nav.faktureringskomponenten.domain.models.forTest
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

    // Test case builders - hides boilerplate and makes tests more readable
    private fun dateTestCase(init: DateTestCaseBuilder.() -> Unit) =
        DateTestCaseBuilder().apply(init).build()

    private fun periodTestCase(init: PeriodTestCaseBuilder.() -> Unit) =
        PeriodTestCaseBuilder().apply(init).build()

    private fun belopTestCase(init: BelopTestCaseBuilder.() -> Unit) =
        BelopTestCaseBuilder().apply(init).build()

    class DateTestCaseBuilder {
        lateinit var name: String
        var dagensDato: LocalDate = LocalDate.of(2024, 1, 1)
        var perioder = mutableListOf<FakturaseriePeriode>()
        var intervall: FakturaserieIntervall = FakturaserieIntervall.KVARTAL
        var expectedDatoBestilt = mutableListOf<LocalDate>()

        fun periode(månedspris: Int, fra: String, til: String, beskrivelse: String = "Test periode") {
            perioder.add(FakturaseriePeriode.forTest {
                this.månedspris = månedspris
                this.fra = fra
                this.til = til
                this.beskrivelse = beskrivelse
            })
        }

        fun forventBestilt(vararg datoer: String) {
            expectedDatoBestilt.addAll(datoer.map { LocalDate.parse(it) })
        }

        fun build() = argumentSet(
            name,
            DateTestCase(name, dagensDato, perioder, intervall, expectedDatoBestilt)
        )
    }

    class PeriodTestCaseBuilder {
        lateinit var name: String
        var dagensDato: LocalDate = LocalDate.of(2024, 1, 1)
        var perioder = mutableListOf<FakturaseriePeriode>()
        var intervall: FakturaserieIntervall = FakturaserieIntervall.KVARTAL
        var expectedPerioder = mutableListOf<Pair<LocalDate, LocalDate>>()

        fun periode(månedspris: Int, fra: String, til: String, beskrivelse: String = "Test periode") {
            perioder.add(FakturaseriePeriode.forTest {
                this.månedspris = månedspris
                this.fra = fra
                this.til = til
                this.beskrivelse = beskrivelse
            })
        }

        fun forventPeriode(fra: String, til: String) {
            expectedPerioder.add(LocalDate.parse(fra) to LocalDate.parse(til))
        }

        fun build() = argumentSet(
            name,
            PeriodTestCase(name, dagensDato, perioder, intervall, expectedPerioder)
        )
    }

    class BelopTestCaseBuilder {
        lateinit var name: String
        var dagensDato: LocalDate = LocalDate.of(2024, 1, 1)
        var perioder = mutableListOf<FakturaseriePeriode>()
        var intervall: FakturaserieIntervall = FakturaserieIntervall.MANEDLIG
        var expectedBelopPerFaktura = mutableListOf<ExpectedBelop>()

        fun periode(månedspris: Int, fra: String, til: String, beskrivelse: String = "Test periode") {
            perioder.add(FakturaseriePeriode.forTest {
                this.månedspris = månedspris
                this.fra = fra
                this.til = til
                this.beskrivelse = beskrivelse
            })
        }

        fun forventBelop(fra: String, til: String, belop: String) {
            expectedBelopPerFaktura.add(
                ExpectedBelop(
                    LocalDate.parse(fra),
                    LocalDate.parse(til),
                    BigDecimal(belop)
                )
            )
        }

        fun build() = argumentSet(
            name,
            BelopTestCase(name, dagensDato, perioder, intervall, expectedBelopPerFaktura)
        )
    }

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
        dateTestCase {
            name = "Kvartalsvis fakturering for fremtidig periode"
            dagensDato = LocalDate.of(2024, 1, 15)
            periode(månedspris = 1000, fra = "2024-04-01", til = "2024-12-31")
            forventBestilt("2024-03-19", "2024-06-19", "2024-09-19")
        },

        dateTestCase {
            name = "Månedsvis fakturering med historiske og fremtidige perioder"
            dagensDato = LocalDate.of(2024, 1, 15)
            intervall = FakturaserieIntervall.MANEDLIG
            periode(månedspris = 1000, fra = "2023-12-01", til = "2024-03-31")
            forventBestilt("2024-01-15", "2024-01-15", "2024-01-19", "2024-02-19")
        },

        dateTestCase {
            name = "Kvartalsvis fakturering over årsskifte"
            dagensDato = LocalDate.of(2023, 12, 20)
            periode(månedspris = 1000, fra = "2023-12-01", til = "2024-03-31")
            forventBestilt("2023-12-20", "2023-12-20")
        }
    )

    private fun periodTestData() = listOf(
        periodTestCase {
            name = "Overlappende perioder"
            periode(månedspris = 1000, fra = "2024-01-01", til = "2024-03-31")
            periode(månedspris = 2000, fra = "2024-03-15", til = "2024-06-30")
            forventPeriode(fra = "2024-01-01", til = "2024-03-31")
            forventPeriode(fra = "2024-04-01", til = "2024-06-30")
        },

        periodTestCase {
            name = "Perioder med opphold"
            periode(månedspris = 1000, fra = "2024-01-01", til = "2024-03-31")
            periode(månedspris = 1000, fra = "2024-07-01", til = "2024-09-30")
            forventPeriode(fra = "2024-01-01", til = "2024-03-31")
            forventPeriode(fra = "2024-07-01", til = "2024-09-30")
        },

        periodTestCase {
            name = "Periode starter midt i måneden"
            intervall = FakturaserieIntervall.MANEDLIG
            periode(månedspris = 1000, fra = "2024-01-15", til = "2024-03-31")
            forventPeriode(fra = "2024-01-15", til = "2024-01-31")
            forventPeriode(fra = "2024-02-01", til = "2024-02-29")
            forventPeriode(fra = "2024-03-01", til = "2024-03-31")
        },

        periodTestCase {
            name = "Skuddår håndtering 2024"
            intervall = FakturaserieIntervall.MANEDLIG
            periode(månedspris = 1000, fra = "2024-02-01", til = "2024-03-15")
            forventPeriode(fra = "2024-02-01", til = "2024-02-29")
            forventPeriode(fra = "2024-03-01", til = "2024-03-15")
        },

        periodTestCase {
            name = "Årsskifte med delvis kvartaler"
            dagensDato = LocalDate.of(2023, 12, 1)
            periode(månedspris = 1000, fra = "2023-11-15", til = "2024-02-15")
            forventPeriode(fra = "2023-11-15", til = "2023-12-31")
            forventPeriode(fra = "2024-01-01", til = "2024-02-15")
        },

        periodTestCase {
            name = "Tre overlappende perioder med ulike priser"
            periode(månedspris = 1000, fra = "2024-01-01", til = "2024-04-30")
            periode(månedspris = 2000, fra = "2024-03-15", til = "2024-07-15")
            periode(månedspris = 3000, fra = "2024-06-01", til = "2024-09-30")
            forventPeriode(fra = "2024-01-01", til = "2024-03-31")
            forventPeriode(fra = "2024-04-01", til = "2024-06-30")
            forventPeriode(fra = "2024-07-01", til = "2024-09-30")
        },

        periodTestCase {
            name = "Periode starter siste dag i måneder med ulik lengde"
            intervall = FakturaserieIntervall.MANEDLIG
            periode(månedspris = 30000, fra = "2024-01-31", til = "2024-04-30")
            forventPeriode(fra = "2024-01-31", til = "2024-01-31")
            forventPeriode(fra = "2024-02-01", til = "2024-02-29")
            forventPeriode(fra = "2024-03-01", til = "2024-03-31")
            forventPeriode(fra = "2024-04-01", til = "2024-04-30")
        },
    )

    private fun belopTestData() = listOf(
        belopTestCase {
            name = "Avrunding for delvis måned med 31 dager"
            periode(månedspris = 31000, fra = "2024-01-15", til = "2024-01-31")
            forventBelop(fra = "2024-01-15", til = "2024-01-31", belop = "17050.00")
        },

        belopTestCase {
            name = "Avrunding for delvis måned med overgang mellom måneder"
            periode(månedspris = 30000, fra = "2024-01-20", til = "2024-01-31")
            forventBelop(fra = "2024-01-20", til = "2024-01-31", belop = "11700.00")
        },

        belopTestCase {
            name = "Avrunding for delvis måned i skuddår (februar)"
            periode(månedspris = 29000, fra = "2024-02-15", til = "2024-02-29")
            forventBelop(fra = "2024-02-15", til = "2024-02-29", belop = "15080.00")
        }
    )
}
