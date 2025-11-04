package no.nav.faktureringskomponenten.domain.models

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Test for FakturaseriePeriodeTestFactory to verify DSL functionality
 */
class FakturaseriePeriodeTestFactoryTest {

    @Test
    fun `should create periode with default values`() {
        val periode = FakturaseriePeriode.forTest { }

        periode.enhetsprisPerManed shouldBe BigDecimal("1000.00")
        periode.startDato shouldBe LocalDate.of(2024, 1, 1)
        periode.sluttDato shouldBe LocalDate.of(2024, 3, 31)
        periode.beskrivelse shouldBe "Test periode"
    }

    @Test
    fun `should create periode with ergonomic aliases`() {
        val periode = FakturaseriePeriode.forTest {
            fra = "2024-01-01"
            til = "2024-03-31"
            månedspris = 5000
            beskrivelse = "Inntekt: 100000, Dekning: Pensjonsdel, Sats: 10%"
        }

        periode.enhetsprisPerManed shouldBe BigDecimal("5000.00")
        periode.startDato shouldBe LocalDate.of(2024, 1, 1)
        periode.sluttDato shouldBe LocalDate.of(2024, 3, 31)
        periode.beskrivelse shouldBe "Inntekt: 100000, Dekning: Pensjonsdel, Sats: 10%"
    }

    @Test
    fun `should create periode with LocalDate directly`() {
        val periode = FakturaseriePeriode.forTest {
            startDato = LocalDate.of(2023, 6, 1)
            sluttDato = LocalDate.of(2023, 12, 31)
            enhetsprisPerManed = BigDecimal(2500)
        }

        periode.enhetsprisPerManed shouldBe BigDecimal("2500.00")
        periode.startDato shouldBe LocalDate.of(2023, 6, 1)
        periode.sluttDato shouldBe LocalDate.of(2023, 12, 31)
    }

    @Test
    fun `should handle BigDecimal scale correctly`() {
        val periode = FakturaseriePeriode.forTest {
            månedspris = 1234
        }

        // Should have 2 decimal places
        periode.enhetsprisPerManed shouldBe BigDecimal("1234.00")
        periode.enhetsprisPerManed.scale() shouldBe 2
    }

    @Test
    fun `should create multiple periods with different values`() {
        val q1 = FakturaseriePeriode.forTest {
            fra = "2024-01-01"
            til = "2024-03-31"
            månedspris = 1000
        }

        val q2 = FakturaseriePeriode.forTest {
            fra = "2024-04-01"
            til = "2024-06-30"
            månedspris = 2000
        }

        q1.enhetsprisPerManed shouldBe BigDecimal("1000.00")
        q2.enhetsprisPerManed shouldBe BigDecimal("2000.00")
        q1.startDato shouldBe LocalDate.of(2024, 1, 1)
        q2.startDato shouldBe LocalDate.of(2024, 4, 1)
    }

    @Test
    fun `should demonstrate readability improvement`() {
        // Old way (verbose):
        // val periode = FakturaseriePeriode(
        //     enhetsprisPerManed = BigDecimal(25470),
        //     startDato = LocalDate.of(2022, 11, 1),
        //     sluttDato = LocalDate.of(2022, 12, 1),
        //     beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
        // )

        // New way (concise and readable):
        val periode = FakturaseriePeriode.forTest {
            månedspris = 25470
            fra = "2022-11-01"
            til = "2022-12-01"
            beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
        }

        periode.enhetsprisPerManed shouldBe BigDecimal("25470.00")
        periode.startDato shouldBe LocalDate.of(2022, 11, 1)
        periode.sluttDato shouldBe LocalDate.of(2022, 12, 1)
    }
}
