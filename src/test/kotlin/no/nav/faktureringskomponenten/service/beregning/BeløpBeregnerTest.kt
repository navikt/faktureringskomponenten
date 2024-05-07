package no.nav.faktureringskomponenten.service.beregning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BeløpBeregnerTest {

    private val enhetspris = BigDecimal("1000.00")

    @Test
    fun regnForPeriode_januar_sisteDelAvMånedRegnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 21)
        val tom = LocalDate.of(2023, 1, 31)


        val result = BeløpBeregner.beløpForPeriode(enhetspris, fom, tom)


        val forventetBeløp = BigDecimal("350.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_november_førsteDelAvMånedRegnesKorrekt() {
        val fom = LocalDate.of(2023, 11, 1)
        val tom = LocalDate.of(2023, 11, 15)


        val result = BeløpBeregner.beløpForPeriode(enhetspris, fom, tom)


        val forventetBeløp = BigDecimal("500.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januarTilDesember_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 21)
        val tom = LocalDate.of(2023, 12, 31)


        val result = BeløpBeregner.beløpForPeriode(enhetspris, fom, tom)


        val forventetBeløp = BigDecimal("11350.00")
        result.shouldBe(forventetBeløp)
    }


    @Test
    fun regnForPeriode_januarTilNovember_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 11, 15)


        val result = BeløpBeregner.beløpForPeriode(enhetspris, fom, tom)


        val forventetBeløp = BigDecimal("10500.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_februarTilDesember_regnesKorrekt() {
        val fom = LocalDate.of(2023, 2, 14)
        val tom = LocalDate.of(2023, 12, 31)


        val result = BeløpBeregner.beløpForPeriode(enhetspris, fom, tom)


        val forventetBeløp = BigDecimal("10540.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januar2023TilHalveFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2024, 2, 15)


        val result = BeløpBeregner.beløpForPeriode(enhetspris, fom, tom)


        val forventetBeløp = BigDecimal("13520.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januar2023TilFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2024, 2, 29)


        val result = BeløpBeregner.beløpForPeriode(enhetspris, fom, tom)


        val forventetBeløp = BigDecimal("14000.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januar2022TilMars2024_regnesKorrekt() {
        val fom = LocalDate.of(2022, 1, 1)
        val tom = LocalDate.of(2024, 3, 31)


        val result = BeløpBeregner.beløpForPeriode(enhetspris, fom, tom)


        val forventetBeløp = BigDecimal("27000.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_skuddÅrFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2024, 2, 1)
        val tom = LocalDate.of(2024, 2, 29)


        val result = BeløpBeregner.beløpForPeriode(enhetspris, fom, tom)


        val forventetBeløp = BigDecimal("1000.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_midtenAvDesember2023TilMidtenAvFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2023, 12, 14)
        val tom = LocalDate.of(2024, 2, 15)


        val result = BeløpBeregner.beløpForPeriode(enhetspris, fom, tom)


        val forventetBeløp = BigDecimal("2100.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun `Avrunding etter 2 desimaler trengs ikke, siden antall bruker 2 desimaler og enhetspris heltall`() {
        val fom = LocalDate.of(2023, 12, 14)
        val tom = LocalDate.of(2024, 2, 15)


        shouldThrow<ArithmeticException> {
            BeløpBeregner.beløpForPeriode(BigDecimal("1002.25"), fom, tom)
        }
    }

    @Test
    fun `Totalbeløp, En periode for hele måneder`() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 5, 31)
        val fakturaseriePeriode = FakturaseriePeriode(BigDecimal.valueOf(700), fom, tom, "dummy")
        val fakturaseriePerioder = listOf(fakturaseriePeriode)

        val result = BeløpBeregner.totalBeløpForAllePerioder(fakturaseriePerioder)

        val forventetBeløp = BigDecimal("3500.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun `Totalbeløp, Ulike perioder med ulik trygdeavgift`() {
        val fom = LocalDate.of(2023, 1, 13)
        val tom = LocalDate.of(2023, 12, 31)
        val fakturaseriePeriode = FakturaseriePeriode(BigDecimal.valueOf(500), fom, tom, "dummy")
        val fom2 = LocalDate.of(2023, 6, 1)
        val tom2 = LocalDate.of(2023, 12, 15)
        val fakturaseriePeriode2 = FakturaseriePeriode(BigDecimal.valueOf(1000), fom2, tom2, "dummy")
        val fakturaseriePerioder = listOf(fakturaseriePeriode, fakturaseriePeriode2)

        val result = BeløpBeregner.totalBeløpForAllePerioder(fakturaseriePerioder)

        val forventetBeløp = BigDecimal("12285.00")
        result.shouldBe(forventetBeløp)
    }
}