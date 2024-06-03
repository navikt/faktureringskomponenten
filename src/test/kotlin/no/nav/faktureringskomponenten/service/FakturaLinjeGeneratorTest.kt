package no.nav.faktureringskomponenten.service

import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FakturaLinjeGeneratorTest {

    @Test
    fun `fra til dato er lik og vi er i første dag av måneden`() {
        val fra = LocalDate.of(2023, 2, 1)
        val til = LocalDate.of(2023, 2, 1)
        val perioder = listOf(
            FakturaseriePeriode(
                enhetsprisPerManed = BigDecimal(25470),
                startDato = LocalDate.of(2022, 12, 1),
                sluttDato = LocalDate.of(2023, 1, 24),
                beskrivelse = "Inntekt: 80000, Dekning: PENSJONSDEL, Sats: 21.5 %"
            ),
            FakturaseriePeriode(
                enhetsprisPerManed = BigDecimal(25470),
                startDato = LocalDate.of(2023, 1, 25),
                sluttDato = LocalDate.of(2023, 2, 1),
                beskrivelse = "Inntekt: 80000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
            )
        )

        val fakturaLinjer = FakturaLinjeGenerator().lagFakturaLinjer(perioder, fra, til)

        fakturaLinjer
            .shouldHaveSize(1)
            .first().apply {
                periodeFra.shouldBe(fra)
                periodeTil.shouldBe(til)
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
            FakturaseriePeriode(
                enhetsprisPerManed = BigDecimal(22830),
                startDato = periodeFraDato,
                sluttDato = periodeTilDato,
                beskrivelse = "Inntekt: 80000, Dekning: Pensjonsdel, Sats: 21.5 %"
            ),
            FakturaseriePeriode(
                enhetsprisPerManed = BigDecimal(25470),
                startDato = periodeFraDato,
                sluttDato = periodeTilDato,
                beskrivelse = "Inntekt: 80000, Dekning: Helse- og pensjonsdel, Sats: 28.3 %"
            )
        )

        val fakturaLinjer = FakturaLinjeGenerator().lagFakturaLinjer(perioder, fakturaFraDato, fakturaTilDato)

        fakturaLinjer.shouldHaveSize(2)
        fakturaLinjer.map { it.periodeFra }.shouldContainOnly(periodeFraDato)
        fakturaLinjer.map { it.periodeTil }.shouldContainOnly(periodeTilDato)
    }
}