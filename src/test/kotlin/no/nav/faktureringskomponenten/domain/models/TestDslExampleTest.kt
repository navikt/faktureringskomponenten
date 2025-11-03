package no.nav.faktureringskomponenten.domain.models

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Eksempeltest som demonstrerer bruk av test DSL for Fakturaserie, Faktura og FakturaLinje.
 *
 * Denne testen viser hvordan DSL-en kan brukes for å lage testdata på en lesbar og konsis måte.
 */
class TestDslExampleTest {

    @Test
    fun `eksempel på enkel faktura med DSL`() {
        // Lager en enkel faktura med standardverdier
        val faktura = Faktura.forTest {
            status = FakturaStatus.BESTILT
        }

        faktura.status shouldBe FakturaStatus.BESTILT
        faktura.fakturaLinje.shouldHaveSize(0)
    }

    @Test
    fun `eksempel på faktura med fakturalinjer`() {
        val faktura = Faktura.forTest {
            status = FakturaStatus.BESTILT
            fakturaLinje {
                fra = "2024-01-01"
                til = "2024-03-31"
                månedspris = 1000
            }
            fakturaLinje {
                fra = "2024-04-01"
                til = "2024-06-30"
                månedspris = 2000
            }
        }

        faktura.fakturaLinje.shouldHaveSize(2)
        faktura.fakturaLinje[0].run {
            periodeFra shouldBe LocalDate.of(2024, 1, 1)
            periodeTil shouldBe LocalDate.of(2024, 3, 31)
            enhetsprisPerManed shouldBe BigDecimal(1000)
            belop shouldBe BigDecimal(1000)
        }
        faktura.fakturaLinje[1].run {
            periodeFra shouldBe LocalDate.of(2024, 4, 1)
            periodeTil shouldBe LocalDate.of(2024, 6, 30)
            enhetsprisPerManed shouldBe BigDecimal(2000)
            belop shouldBe BigDecimal(2000)
        }
    }

    @Test
    fun `eksempel på komplett fakturaserie med nested DSL`() {
        val fakturaserie = Fakturaserie.forTest {
            intervall = FakturaserieIntervall.KVARTAL
            fra = "2024-01-01"
            til = "2024-12-31"

            faktura {
                status = FakturaStatus.BESTILT
                bestilt = "2024-03-19"
                fakturaLinje {
                    fra = "2024-01-01"
                    til = "2024-03-31"
                    månedspris = 1000
                }
                fakturaLinje {
                    fra = "2024-01-01"
                    til = "2024-03-31"
                    månedspris = 2000
                }
            }

            faktura {
                status = FakturaStatus.BESTILT
                bestilt = "2024-06-19"
                fakturaLinje {
                    fra = "2024-04-01"
                    til = "2024-06-30"
                    månedspris = 1000
                }
                fakturaLinje {
                    fra = "2024-04-01"
                    til = "2024-06-30"
                    månedspris = 2000
                }
            }
        }

        fakturaserie.run {
            intervall shouldBe FakturaserieIntervall.KVARTAL
            startdato shouldBe LocalDate.of(2024, 1, 1)
            sluttdato shouldBe LocalDate.of(2024, 12, 31)
            faktura.shouldHaveSize(2)

            // Verifiser at relasjoner er koblet korrekt
            faktura.forEach {
                it.fakturaserie shouldBe fakturaserie
            }

            faktura[0].run {
                status shouldBe FakturaStatus.BESTILT
                datoBestilt shouldBe LocalDate.of(2024, 3, 19)
                fakturaLinje.shouldHaveSize(2)
                totalbeløp() shouldBe BigDecimal(3000)
            }

            faktura[1].run {
                status shouldBe FakturaStatus.BESTILT
                datoBestilt shouldBe LocalDate.of(2024, 6, 19)
                fakturaLinje.shouldHaveSize(2)
                totalbeløp() shouldBe BigDecimal(3000)
            }
        }
    }

    @Test
    fun `eksempel på bruk av hjelpemetoder`() {
        // Bruker ferdiglaget hjelpemetode
        val fakturaserie = FakturaserieTestFactory.lagFakturaserieMedBestilteFakturaer()

        fakturaserie.run {
            status shouldBe FakturaserieStatus.UNDER_BESTILLING
            faktura.shouldHaveSize(2)
            faktura.forEach { it.status shouldBe FakturaStatus.BESTILT }
        }
    }

    @Test
    fun `eksempel på fakturaserie med fullmektig`() {
        val fakturaserie = Fakturaserie.forTest {
            medFullmektig(
                fodselsnummer = "98765432109",
                organisasjonsnummer = "987654321"
            )
        }

        fakturaserie.fullmektig?.run {
            fodselsnummer shouldBe "98765432109"
            organisasjonsnummer shouldBe "987654321"
        }
    }

    @Test
    fun `sammenligning før og etter DSL - avregningsscenario`() {
        // ETTER DSL: Lesbart og konsist
        val faktura2024Q1 = Faktura.forTest {
            status = FakturaStatus.BESTILT
            eksternFakturaNummer = "123"
            fakturaLinje {
                fra = "2024-01-01"
                til = "2024-03-31"
                månedspris = 1000
            }
            fakturaLinje {
                fra = "2024-01-01"
                til = "2024-03-31"
                månedspris = 2000
            }
        }

        val faktura2024Q2 = Faktura.forTest {
            status = FakturaStatus.BESTILT
            eksternFakturaNummer = "456"
            fakturaLinje {
                fra = "2024-04-01"
                til = "2024-06-30"
                månedspris = 1000
            }
            fakturaLinje {
                fra = "2024-04-01"
                til = "2024-06-30"
                månedspris = 2000
            }
        }

        // Før ville dette vært 50+ linjer med manuell konstruksjon
        // Nå: 30 linjer med tydelig intent
        faktura2024Q1.totalbeløp() shouldBe BigDecimal(3000)
        faktura2024Q2.totalbeløp() shouldBe BigDecimal(3000)
    }
}
