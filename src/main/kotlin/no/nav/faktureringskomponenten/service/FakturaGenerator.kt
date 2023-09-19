package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

open class FakturaGenerator(private val fakturalinjeGenerator: FakturalinjeGenerator = FakturalinjeGenerator()) {

    fun lagFakturaerFor(
        startDatoForHelePerioden: LocalDate,
        sluttDatoForHelePerioden: LocalDate,
        fakturaseriePerioder: List<FakturaseriePeriode>,
        faktureringsintervall: FakturaserieIntervall
    ): List<Faktura> {
        val samletFakturaListe = mutableListOf<Faktura>()
        val gjeldendeFakturaLinjer = mutableListOf<FakturaLinje>()
        var gjeldendeFaktureringStartDato = startDatoForHelePerioden

        while (gjeldendeFaktureringStartDato <= sluttDatoForHelePerioden || gjeldendeFakturaLinjer.isNotEmpty()) {
            val gjeldendeFaktureringSluttDato = faktureringSluttDatoFra(gjeldendeFaktureringStartDato, faktureringsintervall)

            val fakturaLinjerForPeriode = lagFakturaLinjerForPeriode(
                gjeldendeFaktureringStartDato,
                gjeldendeFaktureringSluttDato,
                fakturaseriePerioder,
                sluttDatoForHelePerioden
            )

            gjeldendeFakturaLinjer.addAll(fakturaLinjerForPeriode)

            if (dagensDato() <= gjeldendeFaktureringSluttDato) {
                samletFakturaListe.add(tilFakturaTemp(gjeldendeFaktureringStartDato, gjeldendeFakturaLinjer.toList()))
                gjeldendeFakturaLinjer.clear()
            }

            gjeldendeFaktureringStartDato = gjeldendeFaktureringSluttDato.plusDays(1)
        }
        return samletFakturaListe
    }

    private fun faktureringSluttDatoFra(dato: LocalDate, intervall: FakturaserieIntervall): LocalDate {
        if (intervall == FakturaserieIntervall.MANEDLIG) return dato.withDayOfMonth(dato.lengthOfMonth())
        return dato.withMonth(dato[IsoFields.QUARTER_OF_YEAR] * 3).with(TemporalAdjusters.lastDayOfMonth())
    }

    private fun lagFakturaLinjerForPeriode(
        gjeldendeFaktureringStartDato: LocalDate,
        gjeldendeFaktureringSluttDato: LocalDate,
        fakturaseriePerioder: List<FakturaseriePeriode>,
        sluttDatoForHelePerioden: LocalDate
    ): List<FakturaLinje> {
        val fakturaLinjerForPeriode = fakturalinjeGenerator.lagFakturaLinjer(
            perioder = fakturaseriePerioder,
            faktureringFra = gjeldendeFaktureringStartDato,
            faktureringTil = sluttDatoFra(gjeldendeFaktureringSluttDato, sluttDatoForHelePerioden)
        )
        return fakturaLinjerForPeriode
    }

    private fun sluttDatoFra(sisteDagAvPeriode: LocalDate, sluttDatoForHelePerioden: LocalDate) =
        if (sisteDagAvPeriode > sluttDatoForHelePerioden) sluttDatoForHelePerioden else sisteDagAvPeriode

//    private fun tilFaktura(datoBestilt: LocalDate, fakturaLinjer: List<FakturaLinje>): Faktura {
//        val korrigertDatoBestilt = if (datoBestilt <= dagensDato()) dagensDato()
//            .plusDays(BESTILT_DATO_FORSINKES_MED_DAGER) else datoBestilt
//        return Faktura(null, korrigertDatoBestilt, fakturaLinje = fakturaLinjer)
//    }

    // TODO: Før prodsetting, bytt til å bruke tilFaktura. Diskuter med fag hva som er ønsket løsning. Husk også å endre chron-jobb til å gå sjeldnere.
    // Denne er satt til dagensDato slik at testerne kan se umiddelbart alle fakturaene som kommer ut av et vedtak.
    private fun tilFakturaTemp(datoBestilt: LocalDate, fakturaLinjer: List<FakturaLinje>): Faktura {
        return Faktura(null, dagensDato(), fakturaLinje = fakturaLinjer)
    }

    protected open fun dagensDato(): LocalDate = LocalDate.now()

    companion object {
        const val BESTILT_DATO_FORSINKES_MED_DAGER = 0L
    }
}
