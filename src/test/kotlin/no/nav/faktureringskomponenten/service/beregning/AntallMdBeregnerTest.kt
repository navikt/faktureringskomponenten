package no.nav.faktureringskomponenten.service.beregning

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class AntallMdBeregnerTest {
    @Test
    fun `regnAngittAntallForPeriode for Januar regnes rett`() {
        val fom = LocalDate.of(2023, 1, 21)
        val tom = LocalDate.of(2023, 1, 31)


        val result = AntallMdBeregner(fom, tom).beregn()


        val forventetAngittAntall = BigDecimal("0.35")
        result.shouldBe(forventetAngittAntall)
    }

    @Test
    fun `regnAngittAntallForPeriode for November regnes rett`() {
        val fom = LocalDate.of(2023, 11, 1)
        val tom = LocalDate.of(2023, 11, 15)


        val result = AntallMdBeregner(fom, tom).beregn()


        val forventetAngittAntall = BigDecimal("0.50")
        result.shouldBe(forventetAngittAntall)
    }

    @Test
    fun `regnAngittAntallForPeriode for Januar til Desember regnes rett`() {
        val fom = LocalDate.of(2023, 1, 21)
        val tom = LocalDate.of(2023, 12, 31)


        val result = AntallMdBeregner(fom, tom).beregn()


        val forventetAngittAntall = BigDecimal("11.35")
        result.shouldBe(forventetAngittAntall)
    }


    @Test
    fun `regnAngittAntallForPeriode for Januar til November regnes rett`() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 11, 15)


        val result = AntallMdBeregner(fom, tom).beregn()


        val forventetAngittAntall = BigDecimal("10.50")
        result.shouldBe(forventetAngittAntall)
    }

    @Test
    fun `regnAngittAntallForPeriode for Februar til Desember regnes rett`() {
        val fom = LocalDate.of(2023, 2, 14)
        val tom = LocalDate.of(2023, 12, 31)


        val result = AntallMdBeregner(fom, tom).beregn()


        val forventetAngittAntall = BigDecimal("10.54")
        result.shouldBe(forventetAngittAntall)
    }

    @Test
    fun `regnAngittAntallForPeriode for Januar 2023 til Februar 2024 regnes rett og håndterer skuddår`() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2024, 2, 15)


        val result = AntallMdBeregner(fom, tom).beregn()


        val forventetAngittAntall = BigDecimal("13.52")
        result.shouldBe(forventetAngittAntall)
    }

    @Test
    fun `regnAngittAntallForPeriode for Januar 2023 til site dag i Februar 2024 regnes rett`() {
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2024, 2, 29)


        val result = AntallMdBeregner(fom, tom).beregn()


        val forventetAngittAntall = BigDecimal("14.00")
        result.shouldBe(forventetAngittAntall)
    }

    @Test
    fun `regnAngittAntallForPeriode for Januar 2022 til Mars 2024 regnes rett`() {
        val fom = LocalDate.of(2022, 1, 1)
        val tom = LocalDate.of(2024, 3, 31)


        val result = AntallMdBeregner(fom, tom).beregn()


        val forventetAngittAntall = BigDecimal("27.00")
        result.shouldBe(forventetAngittAntall)
    }

    @Test
    fun `regnAngittAntallForPeriode for Februar 2024 regnes rett`() {
        val fom = LocalDate.of(2024, 2, 1)
        val tom = LocalDate.of(2024, 2, 29)


        val result = AntallMdBeregner(fom, tom).beregn()


        val forventetAngittAntall = BigDecimal("1.00")
        result.shouldBe(forventetAngittAntall)
    }

    @Test
    fun `regnAngittAntallForPeriode fra Januar til slutten av Februar 2024 regnes rett`() {
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 2, 29)


        val result = AntallMdBeregner(fom, tom).beregn()


        val forventetAngittAntall = BigDecimal("2.00")
        result.shouldBe(forventetAngittAntall)
    }

    @Test
    fun `regnAngittAntallForPeriode fra midten av Desember 2023 til midten av Februar 2024 regnes rett`() {
        val fom = LocalDate.of(2023, 12, 14)
        val tom = LocalDate.of(2024, 2, 15)


        val result = AntallMdBeregner(fom, tom).beregn()


        val forventetAngittAntall = BigDecimal("2.10")
        result.shouldBe(forventetAngittAntall)
    }

}