package no.nav.faktureringskomponenten.service.avregning

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
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
        val bestilteFakturaer = listOf(faktura2024ForsteKvartal, faktura2024AndreKvartal)
        val fakturaseriePerioder = fakturaseriePerioderTestData()

        val avregningsfaktura = avregningBehandler.lagAvregningsfaktura(fakturaseriePerioder, bestilteFakturaer)

        avregningsfaktura.run {
            sortedBy { it.getPeriodeFra() }
            shouldNotBeNull()
            shouldHaveSize(2)
            get(0).run {
                fakturaLinje[0].shouldBe(
                    FakturaLinje(
                        id = null,
                        periodeFra = LocalDate.of(2024, 1, 1),
                        periodeTil = LocalDate.of(2024, 3, 31),
                        beskrivelse = "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 10000,00 - tidligere beløp: 9000,00",
                        antall = BigDecimal(1),
                        enhetsprisPerManed = BigDecimal("1000.00"),
                        avregningForrigeBeloep = BigDecimal("9000.00"),
                        avregningNyttBeloep = BigDecimal("10000.00"),
                        belop = BigDecimal("1000.00"),
                    )
                )
            }
            get(1).run {
                fakturaLinje[0].shouldBe(
                    FakturaLinje(
                        id = null,
                        periodeFra = LocalDate.of(2024, 4, 1),
                        periodeTil = LocalDate.of(2024, 6, 30),
                        beskrivelse = "Periode: 01.04.2024 - 30.06.2024\nNytt beløp: 12000,00 - tidligere beløp: 9000,00",
                        antall = BigDecimal(1),
                        enhetsprisPerManed = BigDecimal("3000.00"),
                        avregningForrigeBeloep = BigDecimal("9000.00"),
                        avregningNyttBeloep = BigDecimal("12000.00"),
                        belop = BigDecimal("3000.00"),
                    )
                )
            }
        }
    }

    @Test
    fun `lagAvregningsfaktura når bestilte fakturaer inneholder en avregningsfaktura`() {
        val avregningsfaktura =
            avregningBehandler.lagAvregningsfaktura(
                fakturaseriePerioderTestData(),
                listOf(faktura2024ForsteKvartal, faktura2024AndreKvartal)
            )

        avregningsfaktura.run {
            sortedBy { it.getPeriodeFra() }
            filter { it.erAvregningsfaktura() }
            shouldHaveSize(2)
            get(0).referertFakturaVedAvregning.shouldBe(faktura2024ForsteKvartal)
            get(1).referertFakturaVedAvregning.shouldBe(faktura2024AndreKvartal)
        }


        val avregningsfaktura2 =
            avregningBehandler.lagAvregningsfaktura(fakturaseriePerioderTestData2(), avregningsfaktura)

        avregningsfaktura2
            .run {
                sortedBy { it.getPeriodeFra() }
                shouldHaveSize(2)
                get(0).run {
                    referertFakturaVedAvregning.shouldBe(avregningsfaktura[0])
                    krediteringFakturaRef.shouldBe(avregningsfaktura[0].referanseNr)
                    fakturaLinje[0].shouldBe(
                        FakturaLinje(
                            periodeFra = LocalDate.of(2024, 1, 1),
                            periodeTil = LocalDate.of(2024, 3, 31),
                            beskrivelse = "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 11000,00 - tidligere beløp: 10000,00",
                            antall = BigDecimal(1),
                            avregningForrigeBeloep = BigDecimal("10000.00"),
                            avregningNyttBeloep = BigDecimal("11000.00"),
                            enhetsprisPerManed = BigDecimal("1000.00"),
                            belop = BigDecimal("1000.00")
                        )
                    )
                }
                get(1).run {
                    referertFakturaVedAvregning.shouldBe(avregningsfaktura[1])
                    krediteringFakturaRef.shouldBe(avregningsfaktura[1].referanseNr)
                    fakturaLinje[0].shouldBe(
                        FakturaLinje(
                            periodeFra = LocalDate.of(2024, 4, 1),
                            periodeTil = LocalDate.of(2024, 6, 30),
                            beskrivelse = "Periode: 01.04.2024 - 30.06.2024\nNytt beløp: 12000,00 - tidligere beløp: 12000,00",
                            antall = BigDecimal(1),
                            avregningForrigeBeloep = BigDecimal("12000.00"),
                            avregningNyttBeloep = BigDecimal("12000.00"),
                            enhetsprisPerManed = BigDecimal("0.00"),
                            belop = BigDecimal("0.00")
                        )
                    )
                }

            }
    }

    @Test
    fun `lagAvregningsfaktura referer til først positive faktura - første av 2 i dette tilfellet`() {
        val avregningsfaktura =
            avregningBehandler.lagAvregningsfaktura(
                fakturaseriePerioderTestData3(),
                listOf(faktura2024ForsteKvartal, faktura2024AndreKvartal)
            )

        avregningsfaktura.run {
            sortedBy { it.getPeriodeFra() }
            filter { it.erAvregningsfaktura() }
            shouldHaveSize(2)
            get(0).referertFakturaVedAvregning.shouldBe(faktura2024ForsteKvartal)
            get(1).referertFakturaVedAvregning.shouldBe(faktura2024AndreKvartal)
        }


        val avregningsfaktura2 =
            avregningBehandler.lagAvregningsfaktura(fakturaseriePerioderTestData2(), avregningsfaktura)

        avregningsfaktura2
            .run {
                sortedBy { it.getPeriodeFra() }
                shouldHaveSize(2)
                get(0).run {
                    referertFakturaVedAvregning.shouldBe(avregningsfaktura[0])
                    krediteringFakturaRef.shouldBe(faktura2024ForsteKvartal.referanseNr)
                    fakturaLinje[0].shouldBe(
                        FakturaLinje(
                            periodeFra = LocalDate.of(2024, 1, 1),
                            periodeTil = LocalDate.of(2024, 3, 31),
                            beskrivelse = "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 11000,00 - tidligere beløp: −11000,00",
                            antall = BigDecimal(1),
                            avregningForrigeBeloep = BigDecimal("-11000.00"),
                            avregningNyttBeloep = BigDecimal("11000.00"),
                            enhetsprisPerManed = BigDecimal("22000.00"),
                            belop = BigDecimal("22000.00")
                        )
                    )
                }
                get(1).run {
                    referertFakturaVedAvregning.shouldBe(avregningsfaktura[1])
                    krediteringFakturaRef.shouldBe(faktura2024AndreKvartal.referanseNr)
                    fakturaLinje[0].shouldBe(
                        FakturaLinje(
                            periodeFra = LocalDate.of(2024, 4, 1),
                            periodeTil = LocalDate.of(2024, 6, 30),
                            beskrivelse = "Periode: 01.04.2024 - 30.06.2024\nNytt beløp: 12000,00 - tidligere beløp: −12000,00",
                            antall = BigDecimal(1),
                            avregningForrigeBeloep = BigDecimal("-12000.00"),
                            avregningNyttBeloep = BigDecimal("12000.00"),
                            enhetsprisPerManed = BigDecimal("24000.00"),
                            belop = BigDecimal("24000.00")
                        )
                    )
                }

            }
    }

    @Test
    fun `lagAvregningsfaktura krediterer faktura som overlapper med nye perioder`() {
        val avregningsfaktura =
            avregningBehandler.lagAvregningsfaktura(
                fakturaseriePerioderTestData(),
                listOf(faktura2024ForsteKvartal, faktura2024AndreKvartal, faktura2025ForsteKvartal)
            )

        avregningsfaktura.run {
            sortedBy { it.getPeriodeFra() }
            filter { it.erAvregningsfaktura() }
            shouldHaveSize(2)
            get(0).referertFakturaVedAvregning.shouldBe(faktura2024ForsteKvartal)
            get(1).referertFakturaVedAvregning.shouldBe(faktura2024AndreKvartal)
        }
    }


    @Test
    fun `lagAvregningsfaktura krediterer faktura som ikke overlapper med nye perioder der perioden ligger midt i`() {
        val avregningsfaktura =
            avregningBehandler.lagAvregningsfaktura(
                fakturaseriePerioderMedHullIPerioderTestData(),
                listOf(
                    faktura2023ForsteKvartal,
                    faktura2024ForsteKvartal,
                    faktura2024AndreKvartal,
                    faktura2025ForsteKvartal
                )
            )

        avregningsfaktura.run {
            sortedBy { it.getPeriodeFra() }
            filter { it.erAvregningsfaktura() }
            shouldHaveSize(4)
            get(0).referertFakturaVedAvregning.shouldBe(faktura2023ForsteKvartal)
            get(1).referertFakturaVedAvregning.shouldBe(faktura2025ForsteKvartal)
            get(2).referertFakturaVedAvregning.shouldBe(faktura2024ForsteKvartal)
            get(3).referertFakturaVedAvregning.shouldBe(faktura2024AndreKvartal)
        }
    }

    @Test
    fun `lagAvregningsfaktura filtrer bort avregningsfaktura med minusbeløp`() {
        val avregningsfaktura =
            avregningBehandler.lagAvregningsfaktura(
                fakturaseriePerioderTestData(),
                listOf(faktura2024ForsteKvartal, faktura2024AndreKvartal)
            )

        avregningsfaktura.run {
            sortedBy { it.getPeriodeFra() }
            filter { it.erAvregningsfaktura() }
            shouldBeEmpty()
        }
    }

    private val faktura2023ForsteKvartal = Faktura(
        id = 1,
        datoBestilt = LocalDate.of(2023, 3, 19),
        status = FakturaStatus.BESTILT,
        eksternFakturaNummer = "123",
        fakturaLinje = listOf(
            FakturaLinje(
                id = 3,
                periodeFra = LocalDate.of(2023, 1, 1),
                periodeTil = LocalDate.of(2023, 3, 31),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(1000),
                belop = BigDecimal("3000.00"),
            ),
            FakturaLinje(
                id = 4,
                periodeFra = LocalDate.of(2023, 1, 1),
                periodeTil = LocalDate.of(2023, 3, 31),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(2000),
                belop = BigDecimal("6000.00"),
            ),
        ),
    )

    private val faktura2024ForsteKvartal = Faktura(
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

    private val faktura2024AndreKvartal = Faktura(
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

    private val faktura2025ForsteKvartal = Faktura(
        id = 3,
        datoBestilt = LocalDate.of(2025, 3, 19),
        status = FakturaStatus.BESTILT,
        eksternFakturaNummer = "789",
        fakturaLinje = listOf(
            FakturaLinje(
                id = 7,
                periodeFra = LocalDate.of(2025, 1, 1),
                periodeTil = LocalDate.of(2025, 3, 31),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(1000),
                belop = BigDecimal("3000.00"),
            ),
            FakturaLinje(
                id = 8,
                periodeFra = LocalDate.of(2025, 1, 1),
                periodeTil = LocalDate.of(2025, 3, 31),
                beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                antall = BigDecimal(3),
                enhetsprisPerManed = BigDecimal(2000),
                belop = BigDecimal("6000.00"),
            ),
        ),
    )

    private fun fakturaseriePerioderTestData(): List<FakturaseriePeriode> {
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

    private fun fakturaseriePerioderTestData2(): List<FakturaseriePeriode> {
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

    private fun fakturaseriePerioderTestData3(): List<FakturaseriePeriode> {
        return listOf(
            FakturaseriePeriode(
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 12, 31),
                enhetsprisPerManed = BigDecimal.valueOf(-1000),
                beskrivelse = "Dekning: Pensjon og helsedel, Sats 10%"
            ),
            FakturaseriePeriode(
                startDato = LocalDate.of(2024, 1, 1),
                sluttDato = LocalDate.of(2024, 1, 31),
                beskrivelse = "Dekning: Pensjon og helsedel, Sats 10%",
                enhetsprisPerManed = BigDecimal.valueOf(-2000)
            ),
            FakturaseriePeriode(
                startDato = LocalDate.of(2024, 2, 1),
                sluttDato = LocalDate.of(2024, 12, 31),
                enhetsprisPerManed = BigDecimal.valueOf(-3000),
                beskrivelse = "Dekning: Pensjon og helsedel, Sats 10%"
            ),
        )
    }

    private fun fakturaseriePerioderMedHullIPerioderTestData(): List<FakturaseriePeriode> {
        return listOf(
            FakturaseriePeriode(
                startDato = LocalDate.of(2023, 1, 1),
                sluttDato = LocalDate.of(2023, 12, 31),
                enhetsprisPerManed = BigDecimal.valueOf(-1000),
                beskrivelse = "Dekning: Pensjon og helsedel, Sats 10%"
            ),
            FakturaseriePeriode(
                startDato = LocalDate.of(2025, 1, 1),
                sluttDato = LocalDate.of(2025, 12, 31),
                enhetsprisPerManed = BigDecimal.valueOf(-3000),
                beskrivelse = "Dekning: Pensjon og helsedel, Sats 10%"
            ),
        )
    }
}