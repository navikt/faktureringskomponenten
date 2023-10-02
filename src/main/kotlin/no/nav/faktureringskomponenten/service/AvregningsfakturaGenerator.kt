package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import java.math.BigDecimal
import java.time.LocalDate

class AvregningsfakturaGenerator {
    fun lagFaktura(fakturaseriePerioder: List<FakturaseriePeriode>, bestilteFakturaerTilAvregning: List<Faktura>): Faktura {
        val linje = FakturaLinje(
            referertFakturaVedAvregning = bestilteFakturaerTilAvregning[0],
            periodeFra = LocalDate.of(2024, 1, 1),
            periodeTil = LocalDate.of(2024, 3, 31),
            beskrivelse = "it.beskrivelse",
            antall = BigDecimal(1),
            enhetsprisPerManed = BigDecimal(1000),
            belop = BigDecimal(1000),
        )
        return Faktura(null, LocalDate.now(), fakturaLinje = listOf(linje))
    }
}