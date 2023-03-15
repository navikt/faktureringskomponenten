package no.nav.faktureringskomponenten.service.mappers

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import org.junit.jupiter.api.Test

class FakturaBestiltDtoMapperTest {

    @Test
    fun `intervall KVARTAL setter rett beskrivelse`() {
        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(
            Faktura(),
            Fakturaserie(fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT, intervall = FakturaserieIntervall.KVARTAL)
        )

        fakturaBestiltDto
            .shouldNotBeNull()
            .apply {
                beskrivelse.shouldContain("Faktura Trygdeavgift").shouldContain("kvartal")
            }
    }

    @Test
    fun `intervall MANEDLIG setter rett beskrivelse`() {
        val fakturaBestiltDto = FakturaBestiltDtoMapper().tilFakturaBestiltDto(
            Faktura(),
            Fakturaserie(fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT, intervall = FakturaserieIntervall.MANEDLIG)
        )

        fakturaBestiltDto
            .shouldNotBeNull()
            .apply {
                beskrivelse.shouldContain("Faktura Trygdeavgift").shouldNotContain("kvartal")
            }
    }
}