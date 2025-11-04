package no.nav.faktureringskomponenten.service.avregning

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.forTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class AvregningsfakturaGeneratorTest {
    private val generator = AvregningsfakturaGenerator()

    @Test
    fun lagFaktura() {
        val bestilteFaktura = Faktura.forTest {
            eksternFakturaNummer = "123"
        }
        val avregningsperiode = Avregningsperiode(
            bestilteFaktura = bestilteFaktura,
            opprinneligFaktura = bestilteFaktura,
            periodeFra = LocalDate.of(2024, 1, 1),
            periodeTil = LocalDate.of(2024, 3, 31),
            tidligereBeløp = BigDecimal("2000"),
            nyttBeløp = BigDecimal("1000"),
        )

        val faktura = generator.lagFaktura(avregningsperiode)

        faktura.shouldNotBeNull()
        faktura.krediteringFakturaRef.shouldBe(avregningsperiode.bestilteFaktura.referanseNr)

        val fakturaLinje = faktura.fakturaLinje.single()
        fakturaLinje.periodeFra shouldBe LocalDate.of(2024, 1, 1)
        fakturaLinje.periodeTil shouldBe LocalDate.of(2024, 3, 31)
        fakturaLinje.beskrivelse shouldBe "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 1000,00 - tidligere beløp: 2000,00"
        fakturaLinje.antall shouldBe BigDecimal(-1)
        fakturaLinje.enhetsprisPerManed shouldBe BigDecimal(1000)
        fakturaLinje.avregningForrigeBeloep shouldBe BigDecimal(2000)
        fakturaLinje.avregningNyttBeloep shouldBe BigDecimal(1000)
        fakturaLinje.belop shouldBe BigDecimal(-1000)
    }
}