package no.nav.faktureringskomponenten.service.mappers

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.faktureringskomponenten.domain.models.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FakturaBestiltDtoMapperTest {

    @Test
    fun `intervall KVARTAL setter rett beskrivelse`() {
        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(
            Faktura(),
            Fakturaserie(fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT, intervall = FakturaserieIntervall.KVARTAL)
        )

        fakturaBestiltDto.beskrivelse.shouldContain("Faktura Trygdeavgift")
            .shouldContain("kvartal")
    }

    @Test
    fun `intervall MANEDLIG setter rett beskrivelse`() {
        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(
            Faktura(),
            Fakturaserie(fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT, intervall = FakturaserieIntervall.MANEDLIG)
        )

        fakturaBestiltDto.beskrivelse.shouldContain("Faktura Trygdeavgift")
            .shouldNotContain("kvartal")
    }

    @Test
    fun `fakturalinje har rett beskrivelse`() {
        val linje = lagFakturaLinje(false)
        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(
            Faktura(
                fakturaLinje = listOf(linje)
            ),
            Fakturaserie(fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT, intervall = FakturaserieIntervall.MANEDLIG)
        )

        fakturaBestiltDto.fakturaLinjer[0].beskrivelse shouldBe linje.beskrivelse
    }

    @Test
    fun `avregningsfaktura har rett beskrivelse`() {
        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(
            Faktura(
                fakturaLinje = listOf(lagFakturaLinje(true))
            ),
            Fakturaserie(fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT, intervall = FakturaserieIntervall.MANEDLIG)
        )

        fakturaBestiltDto.beskrivelse shouldBe "Faktura for avregning mot tidligere fakturert trygdeavgift"
    }

    @Test
    fun `fakturalinje i avregingsfaktura har rett beskrivelse`() {
        val linje = lagFakturaLinje(true)
        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(
            Faktura(
                fakturaLinje = listOf(linje)
            ),
            Fakturaserie(fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT, intervall = FakturaserieIntervall.MANEDLIG)
        )

        fakturaBestiltDto.fakturaLinjer[0].beskrivelse shouldBe linje.beskrivelse
    }

    private fun lagFakturaLinje(erAvregning: Boolean): FakturaLinje = FakturaLinje(
        referertFakturaVedAvregning = if (erAvregning) Faktura() else null,
        periodeFra = LocalDate.of(2024, 1, 1),
        periodeTil = LocalDate.of(2024, 3, 31),
        beskrivelse = if (erAvregning) {
            "Nytt beløp: 10000,00 - tidligere beløp: 9000,00"
        } else {
            "Inntekt: 30000, Dekning: Helse- og pensjonsdel, Sats:20%"
        },
        antall = BigDecimal(1),
        enhetsprisPerManed = BigDecimal(1000),
        belop = BigDecimal(1000),
    )
}