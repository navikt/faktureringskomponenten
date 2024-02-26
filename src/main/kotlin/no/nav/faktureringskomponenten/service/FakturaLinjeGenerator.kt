package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.service.beregning.AntallMdBeregner
import no.nav.faktureringskomponenten.service.beregning.BeløpBeregner
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class FakturaLinjeGenerator {

    fun lagFakturaLinjer(
        perioder: List<FakturaseriePeriode>,
        faktureringFra: LocalDate,
        faktureringTil: LocalDate
    ): List<FakturaLinje> {
        return perioder.filter {
            it.startDato <= faktureringTil && it.sluttDato >= faktureringFra
        }.map {
            val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
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
                beskrivelse = "Periode: ${fakturaLinjerPeriodeFra.format(FORMATTER)} - ${fakturaLinjerPeriodeTil.format(FORMATTER)}\n${it.beskrivelse}",
                enhetsprisPerManed = it.enhetsprisPerManed
            )
        }.toList()
    }
}
