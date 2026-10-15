package no.nav.faktureringskomponenten.controller.mapper

import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.forTest
import org.junit.jupiter.api.Test

class ModelDtoMapperExtensionsTest {

    @Test
    fun `beskrivelse hentes fra faktura når den er lagret ved bestilling`() {
        val fakturaserie = Fakturaserie.forTest {
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje { månedspris = 1000 }
            }
        }
        fakturaserie.faktura.single().beskrivelse = "Lagret beskrivelse fra bestilling"

        val faktura = fakturaserie.tilFakturaserieResponseDto().faktura.single()

        faktura.beskrivelse.shouldBe("Lagret beskrivelse fra bestilling")
    }

    @Test
    fun `beskrivelse utledes for faktura som ikke er bestilt`() {
        val fakturaserie = Fakturaserie.forTest {
            faktura {
                status = FakturaStatus.OPPRETTET
                fakturaLinje {
                    fra = "2024-01-01"
                    til = "2024-03-31"
                    månedspris = 1000
                }
            }
        }

        val faktura = fakturaserie.tilFakturaserieResponseDto().faktura.single()

        faktura.beskrivelse.shouldBe("Trygdeavgift 1. kvartal 2024")
    }

    @Test
    fun `bestilt faktura uten lagret beskrivelse får ikke utledet beskrivelse`() {
        val fakturaserie = Fakturaserie.forTest {
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje { månedspris = 1000 }
            }
        }

        val faktura = fakturaserie.tilFakturaserieResponseDto().faktura.single()

        faktura.beskrivelse.shouldBe(null)
    }
}
