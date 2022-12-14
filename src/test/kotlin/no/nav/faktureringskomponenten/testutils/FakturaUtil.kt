package no.nav.faktureringskomponenten.testutils

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.testutils.FakturaSerieUtil.Companion.lagFakturaserie
import java.math.BigDecimal
import java.time.LocalDate

class FakturaUtil {

    companion object {
        fun lagFaktura(id: Long? = 1): Faktura {
            return Faktura(
                id,
                LocalDate.of(2022, 5, 1),
                FakturaStatus.OPPRETTET,
                fakturaLinje = listOf(
                    FakturaLinje(
                        100,
                        LocalDate.of(2023, 1, 1),
                        LocalDate.of(2023, 5, 1),
                        beskrivelse = "En beskrivelse",
                        belop = BigDecimal(90000)
                    ),
                )
            ).apply {
                fakturaserie = lagFakturaserie()
            }
        }
    }
}
