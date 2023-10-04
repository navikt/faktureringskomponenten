package no.nav.faktureringskomponenten.service

import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class AvregningBehandlerTest {

    private val avregningBehandler = AvregningBehandler()

    @Test
    fun avregning() {
        val bestilteFakturaer = listOf(faktura1, faktura2)
        val fakturaseriePerioder = fakturaseriePerioder()

        val avregningsfaktura = avregningBehandler.lagAvregningsfaktura(fakturaseriePerioder, bestilteFakturaer)

        avregningsfaktura.shouldNotBeNull().shouldBeEqualToComparingFields(
            Faktura(
                id = null,
                datoBestilt = LocalDate.now(),
                status = FakturaStatus.OPPRETTET,
                fakturaLinje = listOf(
                    FakturaLinje(
                        id = null,
                        referertFakturaVedAvregning = faktura1,
                        periodeFra = LocalDate.of(2024, 1, 1),
                        periodeTil = LocalDate.of(2024, 3, 31),
                        beskrivelse = "nytt beløp: 10000 - tidligere beløp: 9000",
                        antall = BigDecimal(1),
                        enhetsprisPerManed = BigDecimal(1000),
                        belop = BigDecimal(1000),
                    ),
                    FakturaLinje(
                        id = null,
                        referertFakturaVedAvregning = faktura2,
                        periodeFra = LocalDate.of(2024, 4, 1),
                        periodeTil = LocalDate.of(2024, 6, 30),
                        beskrivelse = "nytt beløp: 12000 - tidligere beløp: 9000",
                        antall = BigDecimal(1),
                        enhetsprisPerManed = BigDecimal(3000),
                        belop = BigDecimal(3000),
                    ),
                )
            )
        )
    }

    private val faktura1 = Faktura(
        id = 1,
        datoBestilt = LocalDate.of(2024, 3, 19),
        status = FakturaStatus.BESTILLT,
        fakturaLinje = listOf(
            FakturaLinje(
                id = 3,
                periodeFra = LocalDate.of(2024, 1, 1),
                periodeTil = LocalDate.of(2024, 3, 31),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(1000),
                belop = BigDecimal(3000),
            ),
            FakturaLinje(
                id = 4,
                periodeFra = LocalDate.of(2024, 1, 1),
                periodeTil = LocalDate.of(2024, 3, 31),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(2000),
                belop = BigDecimal(6000),
            ),
        ),
    )

    private val faktura2 = Faktura(
        id = 2,
        datoBestilt = LocalDate.of(2024, 3, 19),
        status = FakturaStatus.BESTILLT,
        fakturaLinje = listOf(
            FakturaLinje(
                id = 5,
                periodeFra = LocalDate.of(2024, 4, 1),
                periodeTil = LocalDate.of(2024, 6, 30),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(1000),
                belop = BigDecimal(3000),
            ),
            FakturaLinje(
                id = 6,
                periodeFra = LocalDate.of(2024, 4, 1),
                periodeTil = LocalDate.of(2024, 6, 30),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(2000),
                belop = BigDecimal(6000),
            ),
        ),
    )

    private fun fakturaseriePerioder(): List<FakturaseriePeriode> {
        return listOf(
            FakturaseriePeriode(
                BigDecimal.valueOf(1000),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                "Dekning: Pensjon og helsedel, Sats 10%"
            ),
            FakturaseriePeriode(
                BigDecimal.valueOf(2000),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 28),
                "Dekning: Pensjon og helsedel, Sats 10%"
            ),
            FakturaseriePeriode(
                BigDecimal.valueOf(3000),
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 12, 31),
                "Dekning: Pensjon og helsedel, Sats 10%"
            ),
        )
    }
}