package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.service.beregning.AntallMdBeregner
import no.nav.faktureringskomponenten.service.beregning.BeløpBeregner
import java.time.LocalDate

class FakturalinjeGenerator {

    fun tilFakturaLinjer(
        perioder: List<FakturaseriePeriode>,
        faktureringFra: LocalDate,
        faktureringTil: LocalDate
    ): List<FakturaLinje> {
        return perioder.filter {
            it.startDato <= faktureringTil && it.sluttDato >= faktureringFra
        }.map {

            val fakturaLinjerPeriodeFra = if (it.startDato < faktureringFra) faktureringFra else it.startDato
            val fakturaLinjerPeriodeTil = if (it.sluttDato >= faktureringTil) faktureringTil else it.sluttDato

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
