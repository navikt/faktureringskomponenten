package no.nav.faktureringskomponenten.service.mappers

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import no.nav.faktureringskomponenten.domain.models.forTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FakturaBestiltDtoMapperTest {

    @Test
    fun `intervall KVARTAL setter rett beskrivelse`() {
        val fakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT
            intervall = FakturaserieIntervall.KVARTAL
            faktura {
                fakturaLinje {
                    beskrivelse = "Inntekt: 30000, Dekning: Helse- og pensjonsdel, Sats:20%"
                }
            }
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.beskrivelse.shouldContain("Faktura Trygdeavgift")
            .shouldContain("kvartal")
    }

    @Test
    fun `Innbetalingstype AARSAVREGNING setter riktig beskrivelse`() {
        val fakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.AARSAVREGNING
            intervall = FakturaserieIntervall.KVARTAL
            faktura {
                fakturaLinje {
                    beskrivelse = "Inntekt: 30000, Dekning: Helse- og pensjonsdel, Sats:20%"
                }
            }
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.beskrivelse.shouldContain("Faktura for oppgjør av trygdeavgift for 2024")
    }

    @Test
    fun `intervall MANEDLIG setter rett beskrivelse`() {
        val fakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT
            intervall = FakturaserieIntervall.MANEDLIG
            faktura {
                fakturaLinje {
                    beskrivelse = "Inntekt: 30000, Dekning: Helse- og pensjonsdel, Sats:20%"
                }
            }
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.beskrivelse.shouldContain("Faktura Trygdeavgift")
            .shouldNotContain("kvartal")
    }

    @Test
    fun `fakturalinje har rett beskrivelse`() {
        val fakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT
            intervall = FakturaserieIntervall.MANEDLIG
            faktura {
                fakturaLinje {
                    beskrivelse = "Inntekt: 30000, Dekning: Helse- og pensjonsdel, Sats:20%"
                }
            }
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.fakturaLinjer[0].beskrivelse shouldBe "Inntekt: 30000, Dekning: Helse- og pensjonsdel, Sats:20%"
    }

    @Test
    fun `avregningsfaktura har rett beskrivelse`() {
        val fakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT
            intervall = FakturaserieIntervall.MANEDLIG
            faktura {
                referertFakturaVedAvregning = Faktura.forTest { }
                fakturaLinje {
                    beskrivelse = "Nytt beløp: 10000,00 - tidligere beløp: 9000,00"
                }
            }
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.beskrivelse shouldBe "Faktura for avregning mot tidligere fakturert trygdeavgift"
    }

    @Test
    fun `fakturalinje i avregingsfaktura har rett beskrivelse`() {
        val fakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT
            intervall = FakturaserieIntervall.MANEDLIG
            faktura {
                referertFakturaVedAvregning = Faktura.forTest { }
                fakturaLinje {
                    beskrivelse = "Nytt beløp: 10000,00 - tidligere beløp: 9000,00"
                }
            }
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.fakturaLinjer[0].beskrivelse shouldBe "Nytt beløp: 10000,00 - tidligere beløp: 9000,00"
    }

    @Test
    fun `faktura har rett beskrivelse for kvartal`() {
        val fakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT
            intervall = FakturaserieIntervall.KVARTAL
            faktura {
                fakturaLinje {
                    fra = "2024-01-01"
                    til = "2024-03-31"
                }
            }
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.beskrivelse shouldBe "Faktura Trygdeavgift 1. kvartal 2024"
    }

    @Test
    fun `faktura har rett beskrivelse for kvartal hvor flere kvartal er slått sammen`() {
        val fakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT
            intervall = FakturaserieIntervall.KVARTAL
            faktura {
                fakturaLinje {
                    fra = "2024-01-01"
                    til = "2024-03-31"
                }
                fakturaLinje {
                    fra = "2024-04-01"
                    til = "2024-06-30"
                }
            }
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.beskrivelse shouldBe "Faktura Trygdeavgift 1.kvartal 2024 - 2.kvartal 2024"
    }

    @Test
    fun `krediteringFakturaRef blir mappet`() {
        val testFakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT
            intervall = FakturaserieIntervall.KVARTAL
            faktura {
                krediteringFakturaRef = "45678913"
                fakturaLinje {
                    beskrivelse = "Test linje"
                }
            }
        }

        val tilFakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(testFakturaserie.faktura.single(), testFakturaserie)
        tilFakturaBestiltDto.krediteringFakturaRef.shouldBe("45678913")
    }

    @Test
    fun `faktureringsDato blir satt til dagens dato`() {
        val fakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT
            intervall = FakturaserieIntervall.MANEDLIG
            faktura {
                datoBestilt = LocalDate.now().minusDays(5)
                fakturaLinje {
                    beskrivelse = "Test fakturalinje"
                }
            }
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.faktureringsDato shouldBe LocalDate.now()
    }

    @Test
    fun `oppgitt kanselleringBeskrivelse brukes som faktura-beskrivelse, linje-beskrivelse er urørt`() {
        val fakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT
            intervall = FakturaserieIntervall.SINGEL
            faktura {
                fakturaLinje {
                    beskrivelse = "Kreditering for periode: 01.01.2024 - 31.12.2024"
                }
            }
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(
                fakturaserie.faktura.single(),
                fakturaserie,
                "Opphør av medlemskap"
            )

        // Faktura-nivå: overstyres av kanselleringBeskrivelse
        fakturaBestiltDto.beskrivelse shouldBe "Opphør av medlemskap"
        // Linje-nivå: kopieres uendret fra fakturalinjen, ikke påvirket av kanselleringBeskrivelse
        fakturaBestiltDto.fakturaLinjer.single().beskrivelse shouldBe "Kreditering for periode: 01.01.2024 - 31.12.2024"
    }

    @Test
    fun `kansellering uten oppgitt beskrivelse faller tilbake på utledet beskrivelse`() {
        val fakturaserie = Fakturaserie.forTest {
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT
            intervall = FakturaserieIntervall.KVARTAL
            faktura {
                fakturaLinje {}
            }
        }

        val fakturaBestiltDto =
            FakturaBestiltDtoMapper().tilFakturaBestiltDto(fakturaserie.faktura.single(), fakturaserie)

        fakturaBestiltDto.beskrivelse shouldBe "Faktura Trygdeavgift 1. kvartal 2024"
    }
}
