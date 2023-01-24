package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.controller.dto.FakturaserieIntervallDto
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

@Component
class FakturaMapper(@Autowired val fakturalinjeMapper: FakturalinjeMapper) {

    companion object {
        val BESTILT_DATO_FORSINKES_MED_DAGER = 1L
    }

    fun tilListeAvFaktura(
        periodeListeDto: List<FakturaseriePeriodeDto>,
        startDatoForHelePerioden: LocalDate,
        sluttDatoForHelePerioden: LocalDate,
        intervall: FakturaserieIntervallDto
    ): List<Faktura> {
        var forsteDagAvPeriode = startDatoForHelePerioden
        var sisteDagAvPeriode = hentSisteDagAvPeriode(startDatoForHelePerioden, intervall)
        val fakturaLinjer = mutableListOf<FakturaLinje>()
        val fakturaListe = mutableListOf<Faktura>()
        val dagensDato = LocalDate.now()
        while (erEldreFaktura(sluttDatoForHelePerioden, forsteDagAvPeriode)) {
            val fakturaLinjerForPeriode = fakturalinjeMapper.tilFakturaLinjer(
                periodeListeDto,
                forsteDagAvPeriode,
                if (sluttDatoForHelePerioden < sisteDagAvPeriode) sluttDatoForHelePerioden else sisteDagAvPeriode
            )

            // Sørger for å samle foregående faktura i én større første faktura
            if (erEldreFaktura(dagensDato, sisteDagAvPeriode)) {
                fakturaLinjer.addAll(fakturaLinjerForPeriode)
            } else {
                if (fakturaLinjer.isNotEmpty()) {
                    fakturaLinjer.addAll(fakturaLinjerForPeriode)
                }

                fakturaListe.add(
                    tilFaktura(
                        forsteDagAvPeriode,
                        if (fakturaLinjer.isNotEmpty()) fakturaLinjer.toList() else fakturaLinjerForPeriode
                    )
                )
                fakturaLinjer.clear()
            }

            forsteDagAvPeriode = sisteDagAvPeriode.plusDays(1)
            sisteDagAvPeriode = hentSisteDagAvPeriode(forsteDagAvPeriode, intervall)

            if (!erEldreFaktura(dagensDato, sisteDagAvPeriode) && fakturaLinjer.isNotEmpty()) {
                fakturaListe.add(
                    tilFaktura(forsteDagAvPeriode, fakturaLinjer.toList())
                )
                fakturaLinjer.clear()
            }
        }
        return fakturaListe
    }

    private fun erEldreFaktura(sluttDatoForHelePerioden: LocalDate, forsteDagAvPeriode: LocalDate) =
        sluttDatoForHelePerioden > forsteDagAvPeriode


    private fun hentSisteDagAvPeriode(dato: LocalDate, intervall: FakturaserieIntervallDto): LocalDate {
        if (intervall == FakturaserieIntervallDto.MANEDLIG)
            return dato.withDayOfMonth(dato.lengthOfMonth())
        return dato.withMonth(dato[IsoFields.QUARTER_OF_YEAR] * 3).with(TemporalAdjusters.lastDayOfMonth())
    }

    fun tilFaktura(datoBestilt: LocalDate, fakturaLinjer: List<FakturaLinje>): Faktura {
        val korrigertDatoBestilt = if (datoBestilt <= LocalDate.now()) LocalDate.now()
            .plusDays(BESTILT_DATO_FORSINKES_MED_DAGER) else datoBestilt
        return Faktura(null, korrigertDatoBestilt, fakturaLinje = fakturaLinjer)
    }
}