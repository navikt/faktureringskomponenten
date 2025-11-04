package no.nav.faktureringskomponenten.domain.models

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Demonstrates the new test factories added in this session.
 * This shows the ergonomic improvements and readability gains.
 */
class NewFactoriesExampleTest {

    @Test
    fun `demonstrate FakturaseriePeriode factory with ergonomic aliases`() {
        // Before: Verbose constructor
        // val periode = FakturaseriePeriode(
        //     enhetsprisPerManed = BigDecimal(25470),
        //     startDato = LocalDate.of(2022, 11, 1),
        //     sluttDato = LocalDate.of(2022, 12, 1),
        //     beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
        // )

        // After: Concise and readable DSL
        val periode = FakturaseriePeriode.forTest {
            månedspris = 25470
            fra = "2022-11-01"
            til = "2022-12-01"
            beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
        }

        periode.enhetsprisPerManed shouldBe BigDecimal("25470.00")
    }

    @Test
    fun `demonstrate EksternFakturaStatus factory`() {
        val fakturaStatus = EksternFakturaStatus.forTest {
            status = FakturaStatus.BESTILT
            datoString = "2024-01-15"
            beløp = 3000
            ubetalt = 1000
        }

        fakturaStatus.status shouldBe FakturaStatus.BESTILT
        fakturaStatus.fakturaBelop shouldBe BigDecimal("3000.00")
        fakturaStatus.ubetaltBelop shouldBe BigDecimal("1000.00")
    }

    @Test
    fun `demonstrate nested ekstern status in Faktura DSL`() {
        val faktura = Faktura.forTest {
            status = FakturaStatus.BESTILT
            eksternFakturaNummer = "EXT-12345"

            fakturaLinje {
                fra = "2024-01-01"
                til = "2024-03-31"
                månedspris = 1000
            }

            // NEW: Can now add ekstern status inline with DSL
            eksternFakturaStatus {
                status = FakturaStatus.BESTILT
                datoString = "2024-01-15"
                beløp = 3000
            }

            eksternFakturaStatus {
                status = FakturaStatus.MANGLENDE_INNBETALING
                datoString = "2024-02-15"
                beløp = 3000
                ubetalt = 1000
            }
        }

        faktura.eksternFakturaStatus.size shouldBe 2
        faktura.eksternFakturaStatus[0].status shouldBe FakturaStatus.BESTILT
        faktura.eksternFakturaStatus[1].status shouldBe FakturaStatus.MANGLENDE_INNBETALING
        faktura.eksternFakturaStatus[1].ubetaltBelop shouldBe BigDecimal("1000.00")
    }

    @Test
    fun `demonstrate full nested structure with all new factories`() {
        // This demonstrates the power of the complete DSL:
        // - Fakturaserie with nested fakturaer
        // - Faktura with nested fakturalinjer and ekstern status
        // - FakturaseriePeriode for generator tests (shown separately)

        val fakturaserie = Fakturaserie.forTest {
            intervall = FakturaserieIntervall.KVARTAL
            fra = "2024-01-01"
            til = "2024-12-31"

            faktura {
                status = FakturaStatus.BESTILT
                bestilt = "2024-01-15"

                fakturaLinje {
                    fra = "2024-01-01"
                    til = "2024-03-31"
                    månedspris = 1000
                }

                eksternFakturaStatus {
                    status = FakturaStatus.BESTILT
                    datoString = "2024-01-15"
                    beløp = 3000
                }
            }

            faktura {
                status = FakturaStatus.OPPRETTET

                fakturaLinje {
                    fra = "2024-04-01"
                    til = "2024-06-30"
                    månedspris = 2000
                }
            }
        }

        fakturaserie.faktura.size shouldBe 2
        fakturaserie.faktura[0].status shouldBe FakturaStatus.BESTILT
        fakturaserie.faktura[0].eksternFakturaStatus.size shouldBe 1
        fakturaserie.faktura[1].status shouldBe FakturaStatus.OPPRETTET
    }

    @Test
    fun `demonstrate FakturaseriePeriode for generator tests`() {
        // This is the BIGGEST win - FakturaseriePeriode is used heavily in
        // FakturaserieGeneratorTest and similar tests with ~100+ constructions

        val perioder = listOf(
            FakturaseriePeriode.forTest {
                månedspris = 10000
                fra = "2022-06-01"
                til = "2023-01-24"
                beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
            },
            FakturaseriePeriode.forTest {
                månedspris = 10000
                fra = "2023-01-25"
                til = "2023-06-01"
                beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
            }
        )

        perioder.size shouldBe 2
        perioder[0].enhetsprisPerManed shouldBe BigDecimal("10000.00")
        perioder[1].enhetsprisPerManed shouldBe BigDecimal("10000.00")
    }
}
