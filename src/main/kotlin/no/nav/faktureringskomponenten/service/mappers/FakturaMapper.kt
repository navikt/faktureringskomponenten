package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

@Component
class FakturaMapper(@Autowired private val fakturalinjeMapper: FakturalinjeMapper) {

    fun tilListeAvFaktura(
        periodeListeDto: List<FakturaseriePeriode>,
        startDatoForHelePerioden: LocalDate,
        sluttDatoForHelePerioden: LocalDate,
        intervall: FakturaserieIntervall
    ): List<Faktura> {
        var forsteDagAvPeriode = startDatoForHelePerioden
        val fakturaLinjer = mutableListOf<FakturaLinje>()
        val fakturaListe = mutableListOf<Faktura>()
        while (sluttDatoForHelePerioden >= forsteDagAvPeriode || fakturaLinjer.isNotEmpty()) {
            val sisteDagAvPeriode = hentSisteDagAvPeriode(forsteDagAvPeriode, intervall)
            val fakturaLinjerForPeriode = fakturalinjeMapper.tilFakturaLinjer(
                perioder = periodeListeDto,
                periodeFra = forsteDagAvPeriode,
                periodeTil = finnSluttDato(sluttDatoForHelePerioden, sisteDagAvPeriode)
            )

            fakturaLinjer.addAll(fakturaLinjerForPeriode)
            if (dagensDato() <= sisteDagAvPeriode) {
                fakturaListe.add(tilFaktura(forsteDagAvPeriode, fakturaLinjer.toList()))
                fakturaLinjer.clear()
            }

            forsteDagAvPeriode = sisteDagAvPeriode.plusDays(1)
        }
        return fakturaListe
    }

    private fun finnSluttDato(sluttDatoForHelePerioden: LocalDate, sisteDagAvPeriode: LocalDate) =
        if (sluttDatoForHelePerioden < sisteDagAvPeriode) sluttDatoForHelePerioden else sisteDagAvPeriode

    private fun hentSisteDagAvPeriode(dato: LocalDate, intervall: FakturaserieIntervall): LocalDate {
        if (intervall == FakturaserieIntervall.MANEDLIG)
            return dato.withDayOfMonth(dato.lengthOfMonth())
        return dato.withMonth(dato[IsoFields.QUARTER_OF_YEAR] * 3).with(TemporalAdjusters.lastDayOfMonth())
    }

    private fun tilFaktura(datoBestilt: LocalDate, fakturaLinjer: List<FakturaLinje>): Faktura {
        val korrigertDatoBestilt = if (datoBestilt <= dagensDato()) dagensDato()
            .plusDays(BESTILT_DATO_FORSINKES_MED_DAGER) else datoBestilt
        return Faktura(null, korrigertDatoBestilt, fakturaLinje = fakturaLinjer)
    }

    // TODO: Før prodsetting, bytt til å bruke tilFaktura. Diskuter med fag hva som er ønsket løsning. Husk også å endre chron-jobb til å gå sjeldnere.
    // Denne er satt til dagensDato slik at testerne kan se umiddelbart alle fakturaene som kommer ut av et vedtak.
//    private fun tilFakturaTemp(datoBestilt: LocalDate, fakturaLinjer: List<FakturaLinje>): Faktura {
//        return Faktura(null, dagensDato(), fakturaLinje = fakturaLinjer)
//    }

    protected fun dagensDato(): LocalDate = LocalDate.now()

    companion object {
        const val BESTILT_DATO_FORSINKES_MED_DAGER = 0L
    }
}
