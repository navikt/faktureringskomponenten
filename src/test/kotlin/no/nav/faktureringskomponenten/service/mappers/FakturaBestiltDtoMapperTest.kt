package no.nav.faktureringskomponenten.service.mappers

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.faktureringskomponenten.FakturaBuilder
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.lagFaktura
import no.nav.faktureringskomponenten.lagFakturaserie
import no.nav.faktureringskomponenten.lagTestFakturalinje
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FakturaBestiltDtoMapperTest {

    @Test
    fun `intervall KVARTAL setter rett beskrivelse`() {
        val fakturaserie = lagFakturaserie {
            fakturaGjelderInnbetalingstype(Innbetalingstype.TRYGDEAVGIFT)
            intervall(FakturaserieIntervall.KVARTAL)
            faktura(
                lagFaktura {
                    fakturaLinje(
                        lagTestFakturalinje {
                            beskrivelse("Inntekt: 30000, Dekning: Helse- og pensjonsdel, Sats:20%")
                        }
                    )
                }
            )
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.beskrivelse.shouldContain("Faktura Trygdeavgift")
            .shouldContain("kvartal")
    }

    @Test
    fun `intervall MANEDLIG setter rett beskrivelse`() {
        val fakturaserie = lagFakturaserie {
            fakturaGjelderInnbetalingstype(Innbetalingstype.TRYGDEAVGIFT)
            intervall(FakturaserieIntervall.MANEDLIG)
            faktura(
                lagFaktura {
                    fakturaLinje(
                        lagTestFakturalinje {
                            beskrivelse("Inntekt: 30000, Dekning: Helse- og pensjonsdel, Sats:20%")
                        }
                    )
                }
            )
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.beskrivelse.shouldContain("Faktura Trygdeavgift")
            .shouldNotContain("kvartal")
    }

    @Test
    fun `fakturalinje har rett beskrivelse`() {
        val fakturaserie = lagFakturaserie {
            fakturaGjelderInnbetalingstype(Innbetalingstype.TRYGDEAVGIFT)
            intervall(FakturaserieIntervall.MANEDLIG)
            faktura(
                lagFaktura {
                    fakturaLinje(
                        lagTestFakturalinje {
                            beskrivelse("Inntekt: 30000, Dekning: Helse- og pensjonsdel, Sats:20%")
                        }
                    )
                }
            )
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.fakturaLinjer[0].beskrivelse shouldBe "Inntekt: 30000, Dekning: Helse- og pensjonsdel, Sats:20%"
    }

    @Test
    fun `avregningsfaktura har rett beskrivelse`() {
        val fakturaserie = lagFakturaserie {
            fakturaGjelderInnbetalingstype(Innbetalingstype.TRYGDEAVGIFT)
            intervall(FakturaserieIntervall.MANEDLIG)
            faktura(
                lagFaktura {
                    fakturaLinje(
                        lagTestFakturalinje {
                            referertFakturaVedAvregning( lagFaktura {  })
                            beskrivelse("Nytt beløp: 10000,00 - tidligere beløp: 9000,00")
                        }
                    )
                }
            )
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.beskrivelse shouldBe "Faktura for avregning mot tidligere fakturert trygdeavgift"
    }

    @Test
    fun `fakturalinje i avregingsfaktura har rett beskrivelse`() {
        val fakturaserie = lagFakturaserie {
            fakturaGjelderInnbetalingstype(Innbetalingstype.TRYGDEAVGIFT)
            intervall(FakturaserieIntervall.MANEDLIG)
            faktura(
                lagFaktura {
                    fakturaLinje(
                        lagTestFakturalinje {
                            referertFakturaVedAvregning( lagFaktura {  })
                            beskrivelse("Nytt beløp: 10000,00 - tidligere beløp: 9000,00")
                        }
                    )
                }
            )
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.fakturaLinjer[0].beskrivelse shouldBe "Nytt beløp: 10000,00 - tidligere beløp: 9000,00"
    }

    @Test
    fun `faktura har rett beskrivelse for kvartal`() {
        val fakturaserie = lagFakturaserie {
            fakturaGjelderInnbetalingstype(Innbetalingstype.TRYGDEAVGIFT)
            intervall(FakturaserieIntervall.KVARTAL)
            faktura(
                lagFaktura {
                    fakturaLinje(
                        lagTestFakturalinje {
                            periodeFra(LocalDate.of(2024, 1, 1))
                            periodeTil(LocalDate.of(2024, 3, 31))
                        }
                    )
                }
            )
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.beskrivelse shouldBe "Faktura Trygdeavgift 1. kvartal 2024"
    }

    @Test
    fun `faktura har rett beskrivelse for kvartal hvor flere kvartal er slått sammen`() {
        val linje = lagFakturaLinje(false)
        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(
            Faktura(
                fakturaLinje = listOf(
                    linje,
                    FakturaLinje(
                        periodeFra = LocalDate.of(2024, 4, 1),
                        periodeTil = LocalDate.of(2024, 6, 30),
                        beskrivelse = "Inntekt: 30000, Dekning: Helse- og pensjonsdel, Sats:20%",
                        antall = BigDecimal(1),
                        enhetsprisPerManed = BigDecimal(1000),
                        belop = BigDecimal(1000),
                    )
                )
            ),
            Fakturaserie(
                fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
                intervall = FakturaserieIntervall.KVARTAL
            )
        )

        fakturaBestiltDto.beskrivelse shouldBe "Faktura Trygdeavgift 1.kvartal 2024 - 2.kvartal 2024"
    }

    @Test
    fun `kreditreferanse blir mappet`() {
        val testFakturaserie = lagFakturaserie {
            faktura(
                lagFaktura {
                    kreditReferanseNr("45678913")
                }
            )
        }

        val tilFakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(testFakturaserie.faktura.single(), testFakturaserie)
        tilFakturaBestiltDto.kreditReferanseNr.shouldBe("45678913")
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