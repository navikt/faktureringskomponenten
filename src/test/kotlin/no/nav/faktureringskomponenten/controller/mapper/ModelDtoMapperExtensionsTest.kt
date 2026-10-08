package no.nav.faktureringskomponenten.controller.mapper

import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.forTest
import org.junit.jupiter.api.Test

class ModelDtoMapperExtensionsTest {

    @Test
    fun `beskrivelse og artikkel hentes fra faktura når de er lagret ved bestilling`() {
        val fakturaserie = Fakturaserie.forTest {
            faktura {
                fakturaLinje { månedspris = 1000 }
            }
        }
        fakturaserie.faktura.single().apply {
            beskrivelse = "Lagret beskrivelse fra bestilling"
            artikkel = "F00008"
        }

        val faktura = fakturaserie.tilFakturaserieResponseDto().faktura.single()

        faktura.beskrivelse.shouldBe("Lagret beskrivelse fra bestilling")
        faktura.artikkel.shouldBe("F00008")
        faktura.beskrivelseErUtledet.shouldBe(false)
    }

    @Test
    fun `beskrivelse og artikkel utledes når de ikke er lagret på fakturaen`() {
        val fakturaserie = Fakturaserie.forTest {
            faktura {
                fakturaLinje {
                    fra = "2024-01-01"
                    til = "2024-03-31"
                    månedspris = 1000
                }
            }
        }

        val faktura = fakturaserie.tilFakturaserieResponseDto().faktura.single()

        faktura.beskrivelse.shouldBe("Trygdeavgift 1. kvartal 2024")
        faktura.artikkel.shouldBe("F00008")
        faktura.beskrivelseErUtledet.shouldBe(true)
    }

    @Test
    fun `kreditnota uten lagret beskrivelse får ikke utledet beskrivelse`() {
        val fakturaserie = Fakturaserie.forTest {
            faktura {
                fakturaLinje { månedspris = 1000 }
            }
        }
        fakturaserie.faktura.single().erKreditnota = true

        val faktura = fakturaserie.tilFakturaserieResponseDto().faktura.single()

        faktura.beskrivelse.shouldBe(null)
        faktura.beskrivelseErUtledet.shouldBe(true)
    }
}
