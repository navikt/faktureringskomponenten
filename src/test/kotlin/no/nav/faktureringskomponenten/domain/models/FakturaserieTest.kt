package no.nav.faktureringskomponenten.domain.models

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.FakturaStatus.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FakturaserieTest {

    @Test
    fun `bestilteFakturaer ekskluderer kreditert faktura og kreditnota`() {
        val fakturaserie = Fakturaserie.forTest {
            status = FakturaserieStatus.UNDER_BESTILLING
            // Korrekt original faktura
            faktura {
                status = BESTILT
                fakturaLinje {
                    fra = "2024-01-01"
                    til = "2024-03-31"
                    månedspris = 1000
                }
            }
            // Feilfaktura som ble kreditert (forblir BESTILT, markert erKreditnota)
            faktura {
                status = BESTILT
                erKreditnota = true
                fakturaLinje {
                    fra = "2024-01-01"
                    til = "2024-03-31"
                    månedspris = 1000
                }
            }
            // Kreditnotaen som nuller ut feilfakturaen
            faktura {
                status = BESTILT
                erKreditnota = true
                fakturaLinje {
                    fra = "2024-01-01"
                    til = "2024-03-31"
                    månedspris = 1000
                    belop = BigDecimal("-3000")
                }
            }
            // Vanlig faktura for q2
            faktura {
                status = BESTILT
                fakturaLinje {
                    fra = "2024-04-01"
                    til = "2024-06-30"
                    månedspris = 1000
                }
            }
        }

        val bestilte = fakturaserie.bestilteFakturaer()

        bestilte.shouldHaveSize(2)
        bestilte.forEach {
            it.erKreditnota shouldBe false
        }
    }
}
