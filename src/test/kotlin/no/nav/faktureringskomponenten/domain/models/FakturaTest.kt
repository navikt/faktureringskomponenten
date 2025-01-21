package no.nav.faktureringskomponenten.domain.models

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import no.nav.faktureringskomponenten.lagFaktura
import no.nav.faktureringskomponenten.lagFakturalinje
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FakturaTest() {

    @Test
    fun `Faktura overlapper med år`() {
        val faktura = lagFaktura {
            fakturaLinje(
                lagFakturalinje {
                    periodeFra(LocalDate.of(2022, 1, 1))
                    periodeTil(LocalDate.of(2022, 5, 1))
                }
            )
        }

        faktura.overlapperMedÅr(2022).shouldBeTrue()
    }

    @Test
    fun `Faktura overlapper ikke med år`() {
        val faktura = lagFaktura {
            fakturaLinje(
                lagFakturalinje {
                    periodeFra(LocalDate.of(2022, 1, 1))
                    periodeTil(LocalDate.of(2022, 5, 1))
                },
                lagFakturalinje {
                    periodeFra(LocalDate.of(2022, 6, 1))
                    periodeTil(LocalDate.of(2022, 9, 1))
                }

            )
        }

        faktura.overlapperMedÅr(2023).shouldBeFalse()
    }
}