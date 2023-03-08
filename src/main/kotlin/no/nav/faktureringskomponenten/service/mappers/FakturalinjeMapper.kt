package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Component
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

            if (fakturaLinjerPeriodeFra > fakturaLinjerPeriodeTil)
                throw IllegalStateException("fakturaLinjerPeriodeFra($fakturaLinjerPeriodeFra) > periodeFra($fakturaLinjerPeriodeTil)")

            FakturaLinje(
                id = null,
                periodeFra = fakturaLinjerPeriodeFra,
                periodeTil = fakturaLinjerPeriodeTil,
                belop = hentBelopForPeriode(it.enhetsprisPerManed, fakturaLinjerPeriodeFra, fakturaLinjerPeriodeTil),
                beskrivelse = it.beskrivelse,
                enhetsprisPerManed = it.enhetsprisPerManed
            )
        }.toList()
    }

    private fun hentBelopForPeriode(
        enhetsprisPerManed: BigDecimal,
        periodeFra: LocalDate,
        periodeTil: LocalDate
    ): BigDecimal {

        val antallManederMellomDatoer = if (periodeFra.monthValue == periodeTil.monthValue) {
            val days = ChronoUnit.DAYS.between(periodeFra, periodeTil) + 1
            days.toDouble() / periodeFra.lengthOfMonth()
        } else {
            val dagerIgjenFraForrigeManed = periodeFra.lengthOfMonth() - periodeFra.dayOfMonth
            val differanseAvManed = periodeTil.monthValue - periodeFra.monthValue - 1
            val dagIManen = periodeTil.dayOfMonth
            dagerIgjenFraForrigeManed.toDouble() / periodeFra.lengthOfMonth() + differanseAvManed + dagIManen.toDouble() / periodeTil.lengthOfMonth()
        }

        return enhetsprisPerManed * BigDecimal.valueOf(antallManederMellomDatoer)
    }

}