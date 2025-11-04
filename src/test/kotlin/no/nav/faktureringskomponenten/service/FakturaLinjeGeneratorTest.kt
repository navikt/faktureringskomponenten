package no.nav.faktureringskomponenten.service

import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.domain.models.forTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FakturaLinjeGeneratorTest {

    @Test
    fun `fra til dato er lik og vi er i første dag av måneden`() {
        val fakturaFra = LocalDate.of(2023, 2, 1)
        val fakturaTil = LocalDate.of(2023, 2, 1)
        val perioder = listOf(
            FakturaseriePeriode.forTest {
                månedspris = 25470
                fra = "2022-12-01"
                til = "2023-01-24"
                beskrivelse = "Inntekt: 80000, Dekning: PENSJONSDEL, Sats: 21.5 %"
            },
            FakturaseriePeriode.forTest {
                månedspris = 25470
                fra = "2023-01-25"
                til = "2023-02-01"
                beskrivelse = "Inntekt: 80000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
            }
        )

        val fakturaLinjer = FakturaLinjeGenerator().lagFakturaLinjer(perioder, fakturaFra, fakturaTil)

        fakturaLinjer
            .shouldHaveSize(1)
            .first().apply {
                periodeFra.shouldBe(fakturaFra)
                periodeTil.shouldBe(fakturaTil)
                beskrivelse.shouldBe("Periode: 01.02.2023 - 01.02.2023\nInntekt: 80000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %")
            }
    }

    @Test
    fun `to perioder med samme fom og tom datoer`() {
        val fakturaFraDato = LocalDate.of(2023, 1, 1)
        val fakturaTilDato = LocalDate.of(2023, 3, 31)
        val periodeFraDato = LocalDate.of(2023, 2, 1)
        val periodeTilDato = LocalDate.of(2023, 3, 31)

        val perioder = listOf(
            FakturaseriePeriode.forTest {
                månedspris = 22830
                this.startDato = periodeFraDato
                this.sluttDato = periodeTilDato
                beskrivelse = "Inntekt: 80000, Dekning: Pensjonsdel, Sats: 21.5 %"
            },
            FakturaseriePeriode.forTest {
                månedspris = 25470
                this.startDato = periodeFraDato
                this.sluttDato = periodeTilDato
                beskrivelse = "Inntekt: 80000, Dekning: Helse- og pensjonsdel, Sats: 28.3 %"
            }
        )

        val fakturaLinjer = FakturaLinjeGenerator().lagFakturaLinjer(perioder, fakturaFraDato, fakturaTilDato)

        fakturaLinjer.shouldHaveSize(2)
        fakturaLinjer.map { it.periodeFra }.shouldContainOnly(periodeFraDato)
        fakturaLinjer.map { it.periodeTil }.shouldContainOnly(periodeTilDato)
    }
}