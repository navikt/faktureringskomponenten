package no.nav.faktureringskomponenten.service.avregning

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BeløpAvregnerTest {

    val enhetspris_1000 = BigDecimal("1000.00")

    @Test
    fun regnForPeriode_januar_sisteDelAvMånedRegnesKorrekt() {
        val forventetBeløp = BigDecimal("350.00")
        val fom = LocalDate.of(2023, 1, 21)
        val tom = LocalDate.of(2023, 1, 31)


        val result = BeløpAvregner.regnForPeriode(enhetspris_1000, fom, tom)


        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_november_førsteDelAvMånedRegnesKorrekt() {
        val forventetBeløp = BigDecimal("500.00")
        val fom = LocalDate.of(2023, 11, 1)
        val tom = LocalDate.of(2023, 11, 15)


        val result = BeløpAvregner.regnForPeriode(enhetspris_1000, fom, tom)


        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_januarTilDesember_regnesKorrekt() {
        val forventetBeløp = BigDecimal("11350.00")
        val fom = LocalDate.of(2023, 1, 21)
        val tom = LocalDate.of(2023, 12, 31)


        val result = BeløpAvregner.regnForPeriode(enhetspris_1000, fom, tom)


        result.shouldBe(forventetBeløp)
    }


    @Test
    fun regnForPeriode_januarTilNovember_regnesKorrekt() {
        val forventetBeløp = BigDecimal("10500.00")
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 11, 15)


        val result = BeløpAvregner.regnForPeriode(enhetspris_1000, fom, tom)


        result.shouldBe(forventetBeløp)
    }

    @Test
    fun regnForPeriode_februarTilDesember_regnesKorrekt() {
        val forventetBeløp = BigDecimal("10540.00")
        val fom = LocalDate.of(2023, 2, 14)
        val tom = LocalDate.of(2023, 12, 31)


        val result = BeløpAvregner.regnForPeriode(enhetspris_1000, fom, tom)


        result.shouldBe(forventetBeløp)
    }
}