package no.nav.faktureringskomponenten.service.avregning

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class AvregningsfakturaGeneratorTest {
    private val generator = AvregningsfakturaGenerator()

    @Test
    fun `skal ikke generere faktura uten avregningsperiode`() {
        val faktura = generator.lagFaktura(emptyList())
        faktura shouldBe null
    }

    @Test
    fun lagFaktura() {
        val bestilteFaktura = Faktura()
        val avregningsperiode = Avregningsperiode(
            bestilteFaktura = bestilteFaktura,
            periodeFra = LocalDate.of(2024, 1, 1),
            periodeTil = LocalDate.of(2024, 3, 31),
            tidligereBeløp = BigDecimal("2000"),
            nyttBeløp = BigDecimal("1000")
        )

        val faktura = generator.lagFaktura(listOf(avregningsperiode))

        faktura.shouldNotBeNull()
        faktura.fakturaLinje shouldContain FakturaLinje(
            id = null,
            referertFakturaVedAvregning = bestilteFaktura,
            periodeFra = LocalDate.of(2024, 1, 1),
            periodeTil = LocalDate.of(2024, 3, 31),
            beskrivelse = "nytt beløp: 1000,00 - tidligere beløp: 2000,00",
            antall = BigDecimal(1),
            enhetsprisPerManed = BigDecimal(-1000),
            belop = BigDecimal(-1000),
        )
    }
}