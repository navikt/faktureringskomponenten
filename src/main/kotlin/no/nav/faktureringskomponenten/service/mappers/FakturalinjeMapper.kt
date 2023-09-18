package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.service.beregning.AntallMdBeregner
import no.nav.faktureringskomponenten.service.beregning.BeløpBeregner
import java.time.LocalDate

class FakturalinjeMapper {

    fun tilFakturaLinjer(
        perioder: List<FakturaseriePeriode>,
        periodeFra: LocalDate,
        periodeTil: LocalDate
    ): List<FakturaLinje> {
        return perioder.filter {
            it.startDato <= periodeTil && it.sluttDato >= periodeFra
        }.map {

            val fakturaLinjerPeriodeFra = if (it.startDato < periodeFra) periodeFra else it.startDato
            val fakturaLinjerPeriodeTil = if (it.sluttDato >= periodeTil) periodeTil else it.sluttDato

            check(fakturaLinjerPeriodeFra <= fakturaLinjerPeriodeTil) { "fakturaLinjerPeriodeFra($fakturaLinjerPeriodeFra) > periodeFra($fakturaLinjerPeriodeTil)" }

            FakturaLinje(
                id = null,
                periodeFra = fakturaLinjerPeriodeFra,
                periodeTil = fakturaLinjerPeriodeTil,
                belop = BeløpBeregner.beløpForPeriode(
                    it.enhetsprisPerManed,
                    fakturaLinjerPeriodeFra,
                    fakturaLinjerPeriodeTil
                ),
                antall = AntallMdBeregner(fakturaLinjerPeriodeFra, fakturaLinjerPeriodeTil).beregn(),
                beskrivelse = it.beskrivelse,
                enhetsprisPerManed = it.enhetsprisPerManed
            )
        }.toList()
    }
}
