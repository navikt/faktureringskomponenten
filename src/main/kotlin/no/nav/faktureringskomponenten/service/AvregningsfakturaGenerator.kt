package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import java.math.BigDecimal
import java.time.LocalDate

class AvregningsfakturaGenerator {
    fun lagFaktura(fakturaseriePerioder: List<FakturaseriePeriode>, bestilteFakturaerTilAvregning: List<Faktura>): Faktura {
        val fakturaLinjer = listOf(
            FakturaLinje(
                id = null,
                referertFakturaVedAvregning = bestilteFakturaerTilAvregning[0],
                periodeFra = LocalDate.of(2024, 1, 1),
                periodeTil = LocalDate.of(2024, 3, 31),
                beskrivelse = "nytt beløp: 10.000 - tidligere beløp: 9.000",
                antall = BigDecimal(1),
                enhetsprisPerManed = BigDecimal(1000),
                belop = BigDecimal(1000),
            ),
            FakturaLinje(
                id = null,
                referertFakturaVedAvregning = bestilteFakturaerTilAvregning[1],
                periodeFra = LocalDate.of(2024, 4, 1),
                periodeTil = LocalDate.of(2024, 6, 30),
                beskrivelse = "nytt beløp: 12.000 - tidligere beløp: 9.000",
                antall = BigDecimal(1),
                enhetsprisPerManed = BigDecimal(3000),
                belop = BigDecimal(3000),
            )
        )
        return Faktura(fakturaLinje = fakturaLinjer)
    }
}