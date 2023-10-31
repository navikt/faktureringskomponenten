package no.nav.faktureringskomponenten.service.avregning

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class AvregningBehandlerTest {

    private val avregningBehandler = AvregningBehandler(AvregningsfakturaGenerator())

    @Test
    fun lagAvregningsfaktura() {
        val bestilteFakturaer = listOf(faktura1, faktura2)
        val fakturaseriePerioder = fakturaseriePerioder2()

        val avregningsfaktura = avregningBehandler.lagAvregningsfaktura(fakturaseriePerioder, bestilteFakturaer)

        avregningsfaktura.shouldNotBeNull()
        avregningsfaktura.fakturaLinje shouldBe listOf(
            FakturaLinje(
                id = null,
                referertFakturaVedAvregning = faktura2,
                periodeFra = LocalDate.of(2024, 4, 1),
                periodeTil = LocalDate.of(2024, 6, 30),
                beskrivelse = "Periode: 01.04.2024 - 30.06.2024\nNytt beløp: 12000,00 - tidligere beløp: 9000,00",
                antall = BigDecimal(1),
                enhetsprisPerManed = BigDecimal("3000.00"),
                avregningForrigeBeloep = BigDecimal("9000.00"),
                avregningNyttBeloep = BigDecimal("12000.00"),
                belop = BigDecimal("3000.00"),
            ),
            FakturaLinje(
                id = null,
                referertFakturaVedAvregning = faktura1,
                periodeFra = LocalDate.of(2024, 1, 1),
                periodeTil = LocalDate.of(2024, 3, 31),
                beskrivelse = "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 10000,00 - tidligere beløp: 9000,00",
                antall = BigDecimal(1),
                enhetsprisPerManed = BigDecimal("1000.00"),
                avregningForrigeBeloep = BigDecimal("9000.00"),
                avregningNyttBeloep = BigDecimal("10000.00"),
                belop = BigDecimal("1000.00"),
            ),
        )
    }

    @Test
    fun `lagAvregningsfaktura når bestilte fakturaer inneholder en avregningsfaktura`() {
        val avregningsfaktura = avregningBehandler.lagAvregningsfaktura(fakturaseriePerioder2(), listOf(faktura1, faktura2))

        val avregningsfaktura2 = avregningBehandler.lagAvregningsfaktura(fakturaseriePerioder3(), listOf(avregningsfaktura!!))

        avregningsfaktura2.shouldNotBeNull()
        avregningsfaktura2.fakturaLinje.single() shouldBe FakturaLinje(
            periodeFra = LocalDate.of(2024, 1, 1),
            periodeTil = LocalDate.of(2024, 3, 31),
            beskrivelse = "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 11000,00 - tidligere beløp: 10000,00",
            antall = BigDecimal(1),
            avregningForrigeBeloep = BigDecimal("10000.00"),
            avregningNyttBeloep = BigDecimal("11000.00"),
            enhetsprisPerManed = BigDecimal("1000.00"),
            belop = BigDecimal("1000.00"),
        )
    }

    private val faktura1 = Faktura(
        id = 1,
        datoBestilt = LocalDate.of(2024, 3, 19),
        status = FakturaStatus.BESTILT,
        eksternFakturaNummer = "123",
        fakturaLinje = listOf(
            FakturaLinje(
                id = 3,
                periodeFra = LocalDate.of(2024, 1, 1),
                periodeTil = LocalDate.of(2024, 3, 31),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(1000),
                belop = BigDecimal("3000.00"),
            ),
            FakturaLinje(
                id = 4,
                periodeFra = LocalDate.of(2024, 1, 1),
                periodeTil = LocalDate.of(2024, 3, 31),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(2000),
                belop = BigDecimal("6000.00"),
            ),
        ),
    )

    private val faktura2 = Faktura(
        id = 2,
        datoBestilt = LocalDate.of(2024, 3, 19),
        status = FakturaStatus.BESTILT,
        eksternFakturaNummer = "456",
        fakturaLinje = listOf(
            FakturaLinje(
                id = 5,
                periodeFra = LocalDate.of(2024, 4, 1),
                periodeTil = LocalDate.of(2024, 6, 30),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(1000),
                belop = BigDecimal("3000.00"),
            ),
            FakturaLinje(
                id = 6,
                periodeFra = LocalDate.of(2024, 4, 1),
                periodeTil = LocalDate.of(2024, 6, 30),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(2000),
                belop = BigDecimal("6000.00"),
            ),
        ),
    )

    private fun fakturaseriePerioder2(): List<FakturaseriePeriode> {
        return listOf(
            FakturaseriePeriode(
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 12, 31),
                enhetsprisPerManed = BigDecimal.valueOf(1000),
                beskrivelse = "Dekning: Pensjon og helsedel, Sats 10%"
            ),
            FakturaseriePeriode(
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 2, 29),
                beskrivelse = "Dekning: Pensjon og helsedel, Sats 10%",
                enhetsprisPerManed = BigDecimal.valueOf(2000)
            ),
            FakturaseriePeriode(
                startDato = LocalDate.of(2024, 3, 1),
                sluttDato = LocalDate.of(2024, 12, 31),
                enhetsprisPerManed = BigDecimal.valueOf(3000),
                beskrivelse = "Dekning: Pensjon og helsedel, Sats 10%"
            ),
        )
    }

    private fun fakturaseriePerioder3(): List<FakturaseriePeriode> {
        return listOf(
            FakturaseriePeriode(
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 12, 31),
                enhetsprisPerManed = BigDecimal.valueOf(1000),
                beskrivelse = "Dekning: Pensjon og helsedel, Sats 10%"
            ),
            FakturaseriePeriode(
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 1, 31),
                beskrivelse = "Dekning: Pensjon og helsedel, Sats 10%",
                enhetsprisPerManed = BigDecimal.valueOf(2000)
            ),
            FakturaseriePeriode(
                startDato = LocalDate.of(2024, 2, 1),
                sluttDato = LocalDate.of(2024, 12, 31),
                enhetsprisPerManed = BigDecimal.valueOf(3000),
                beskrivelse = "Dekning: Pensjon og helsedel, Sats 10%"
            ),
        )
    }
}