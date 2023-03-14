package no.nav.faktureringskomponenten.service.beregning

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BeløpBeregnerTest {

    val enhetspris_1000 = BigDecimal("1000.00")

    @Test
    fun regnForPeriode_januar_sisteDelAvMånedRegnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 21)
        val tom = LocalDate.of(2023, 1, 31)


        val result = BeløpBeregner.beløpForPeriode(enhetspris_1000, fom, tom)


        val forventetBeløp = BigDecimal("350.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_november_førsteDelAvMånedRegnesKorrekt() {
        val fom = LocalDate.of(2023, 11, 1)
        val tom = LocalDate.of(2023, 11, 15)


        val result = BeløpBeregner.beløpForPeriode(enhetspris_1000, fom, tom)


        val forventetBeløp = BigDecimal("500.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januarTilDesember_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 21)
        val tom = LocalDate.of(2023, 12, 31)


        val result = BeløpBeregner.beløpForPeriode(enhetspris_1000, fom, tom)


        val forventetBeløp = BigDecimal("11350.00")
        result.shouldBe(forventetBeløp)
    }


    @Test
    fun regnForPeriode_januarTilNovember_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 11, 15)


        val result = BeløpBeregner.beløpForPeriode(enhetspris_1000, fom, tom)


        val forventetBeløp = BigDecimal("10500.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_februarTilDesember_regnesKorrekt() {
        val fom = LocalDate.of(2023, 2, 14)
        val tom = LocalDate.of(2023, 12, 31)


        val result = BeløpBeregner.beløpForPeriode(enhetspris_1000, fom, tom)


        val forventetBeløp = BigDecimal("10540.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januar2023TilHalveFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2024, 2, 15)


        val result = BeløpBeregner.beløpForPeriode(enhetspris_1000, fom, tom)


        val forventetBeløp = BigDecimal("13520.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januar2023TilFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2024, 2, 29)


        val result = BeløpBeregner.beløpForPeriode(enhetspris_1000, fom, tom)


        val forventetBeløp = BigDecimal("14000.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januar2022TilMars2024_regnesKorrekt() {
        val fom = LocalDate.of(2022, 1, 1)
        val tom = LocalDate.of(2024, 3, 31)


        val result = BeløpBeregner.beløpForPeriode(enhetspris_1000, fom, tom)


        val forventetBeløp = BigDecimal("27000.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_skuddÅrFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2024, 2, 1)
        val tom = LocalDate.of(2024, 2, 29)


        val result = BeløpBeregner.beløpForPeriode(enhetspris_1000, fom, tom)


        val forventetBeløp = BigDecimal("1000.00")
        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_midtenAvDesember2023TilMidtenAvFebruar2024_regnesKorrekt() {
        val fom = LocalDate.of(2023, 12, 14)
        val tom = LocalDate.of(2024, 2, 15)


        val result = BeløpBeregner.beløpForPeriode(enhetspris_1000, fom, tom)


        val forventetBeløp = BigDecimal("2100.00")
        result.shouldBe(forventetBeløp)
    }
}