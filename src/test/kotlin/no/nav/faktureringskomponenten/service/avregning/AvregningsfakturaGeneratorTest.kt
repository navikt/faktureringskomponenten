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
    fun lagFaktura() {
        val bestilteFaktura = Faktura(eksternFakturaNummer = "123")
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
        faktura.fakturaLinje shouldContain FakturaLinje(
            id = null,
            periodeFra = LocalDate.of(2024, 1, 1),
            periodeTil = LocalDate.of(2024, 3, 31),
            beskrivelse = "Periode: 01.01.2024 - 31.03.2024\nNytt beløp: 1000,00 - tidligere beløp: 2000,00",
            antall = BigDecimal(-1),
            enhetsprisPerManed = BigDecimal(1000),
            avregningForrigeBeloep = avregningsperiode.tidligereBeløp,
            avregningNyttBeloep = avregningsperiode.nyttBeløp,
            belop = BigDecimal(-1000),
        )
    }
}