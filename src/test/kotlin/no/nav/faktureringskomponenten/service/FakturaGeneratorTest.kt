package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.time.LocalDate

class FakturaGeneratorTest {
    private val fakturaLinjeGenerator = mock<FakturaLinjeGenerator>()
    private val unleash = FakeUnleash()
    private val generator = FakturaGenerator(fakturaLinjeGenerator, unleash)

    private val nesteÅr = LocalDate.now().plusYears(1).year

    @Test
    fun `DatoBestilt på faktura tilbake i tid settes til dagens dato hvis periode starter før dagens dato`() {
        val faktura = generator.lagFakturaerFor(
            LocalDate.now(),
            LocalDate.now().plusMonths(3),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.now(),
                    sluttDato = LocalDate.now().plusMonths(3),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaserieIntervall.KVARTAL
        )
        faktura.first().datoBestilt.shouldBe(LocalDate.now())
    }

    @Test
    fun `DatoBestilt på faktura frem i tid settes til 19 i måneden før kvartalet perioden gjelder for`() {
        val faktura = generator.lagFakturaerFor(
            LocalDate.of(nesteÅr, 1, 1),
            LocalDate.of(nesteÅr, 5, 20),
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(nesteÅr, 1, 1),
                    sluttDato = LocalDate.of(nesteÅr, 3, 31),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(15000),
                    startDato = LocalDate.of(nesteÅr, 4, 1),
                    sluttDato = LocalDate.of(nesteÅr, 5, 20),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaserieIntervall.KVARTAL
        )
        faktura.sortedBy { it.datoBestilt }.map { it.datoBestilt }
            .shouldContainInOrder(LocalDate.of(nesteÅr - 1, 12, 19), LocalDate.of(nesteÅr, 3, 19))
    }
}