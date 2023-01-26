package no.nav.faktureringskomponenten.service.mappers

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FakturalinjeMapperTest {

    @Test
    fun `fra til dato er lik og vi er i første dag av måneden`() {
        val fra = LocalDate.of(2023, 2, 1)
        val til = LocalDate.of(2023, 2, 1)
        val perioder = listOf(
            FakturaseriePeriodeDto(
                enhetsprisPerManed = BigDecimal(25470),
                startDato = LocalDate.of(2022, 12, 1),
                sluttDato = LocalDate.of(2023, 1, 24),
                beskrivelse = "Inntekt: 80000, Dekning: PENSJONSDEL, Sats: 21.5 %"
            ),
            FakturaseriePeriodeDto(
                enhetsprisPerManed = BigDecimal(25470),
                startDato = LocalDate.of(2023, 1, 25),
                sluttDato = LocalDate.of(2023, 2, 1),
                beskrivelse = "Inntekt: 80000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
            )
        )

        val fakturaLinjer = FakturalinjeMapper().tilFakturaLinjer(perioder, fra, til)

        fakturaLinjer
            .shouldHaveSize(1)
            .first().apply {
                periodeFra.shouldBe(fra)
                periodeTil.shouldBe(til)
                belop.toInt().shouldBe(909)
            }
    }
}