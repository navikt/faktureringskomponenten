package no.nav.faktureringskomponenten.service

import io.getunleash.Unleash
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.springframework.stereotype.Component
import ulid.ULID
import java.time.LocalDate
import java.time.Month
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

@Component
class FakturaGenerator (
    private val fakturalinjeGenerator: FakturaLinjeGenerator,
    private val unleash: Unleash
) {

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

            if (skalLageFakturaForPeriode(dagensDato(), gjeldendeFaktureringSluttDato)) {
                var faktura = tilFaktura(gjeldendeFaktureringStartDato, gjeldendeFakturaLinjer.toList())

                if(unleash.isEnabled("melosys.faktureringskomponent.send_faktura_instant")){
                    faktura = tilFakturaTemp(gjeldendeFakturaLinjer.toList())
                }

                samletFakturaListe.add(faktura)
                gjeldendeFakturaLinjer.clear()
            }

            gjeldendeFaktureringStartDato = gjeldendeFaktureringSluttDato.plusDays(1)
        }
        return samletFakturaListe
    }

    private fun faktureringSluttDatoFra(startDato: LocalDate, intervall: FakturaserieIntervall): LocalDate {
        var sluttDato = if (intervall == FakturaserieIntervall.MANEDLIG) {
            startDato.withDayOfMonth(startDato.lengthOfMonth())
        } else {
            startDato.withMonth(startDato[IsoFields.QUARTER_OF_YEAR] * 3).with(TemporalAdjusters.lastDayOfMonth())
        }

        if (startDato.year != sluttDato.year) {
            sluttDato = LocalDate.of(startDato.year, 12, 31)
        }

        return sluttDato
    }

    private fun lagFakturaLinjerForPeriode(
        gjeldendeFaktureringStartDato: LocalDate,
        gjeldendeFaktureringSluttDato: LocalDate,
        fakturaseriePerioder: List<FakturaseriePeriode>,
        sluttDatoForHelePerioden: LocalDate
    ): List<FakturaLinje> = fakturalinjeGenerator.lagFakturaLinjer(
        perioder = fakturaseriePerioder,
        faktureringFra = gjeldendeFaktureringStartDato,
        faktureringTil = sluttDatoFra(gjeldendeFaktureringSluttDato, sluttDatoForHelePerioden)
    )

    private fun sluttDatoFra(sisteDagAvPeriode: LocalDate, sluttDatoForHelePerioden: LocalDate) =
        if (sisteDagAvPeriode > sluttDatoForHelePerioden) sluttDatoForHelePerioden else sisteDagAvPeriode

    private fun skalLageFakturaForPeriode(
        dagensDato: LocalDate,
        gjeldendeFaktureringSluttDato: LocalDate
    ): Boolean = dagensDato <= gjeldendeFaktureringSluttDato || erSisteDagIÅret(gjeldendeFaktureringSluttDato)

    private fun erSisteDagIÅret(dato: LocalDate): Boolean = dato.month == Month.DECEMBER && dato.dayOfMonth == 31

    private fun tilFaktura(fakturaStartDato: LocalDate, fakturaLinjer: List<FakturaLinje>): Faktura {
        val bestillingsdato = utledBestillingsdato(fakturaStartDato)
        return Faktura(
            null,
            referanseNr = ULID.randomULID(),
            datoBestilt = bestillingsdato,
            sistOppdatert = bestillingsdato,
            fakturaLinje = fakturaLinjer.sortedByDescending { it.periodeFra })
    }

    private fun erNesteKvartalOgKvartalsbestillingHarKjørt(
        fakturaStartDato: LocalDate,
        dagensDato: LocalDate,
    ): Boolean {
        val erNesteKvartal = dagensDato < fakturaStartDato && dagensDato[IsoFields.QUARTER_OF_YEAR]
            .plus(1) % 4 == fakturaStartDato[IsoFields.QUARTER_OF_YEAR] % 4
        val sisteMånedIDagensKvartal = dagensDato.month.firstMonthOfQuarter().plus(2)
        val kvartalsBestilingHarKjørt =
            dagensDato > LocalDate.now().withMonth(sisteMånedIDagensKvartal.value).withDayOfMonth(19)
        return erNesteKvartal && kvartalsBestilingHarKjørt
    }

    private fun utledBestillingsdato(fakturaStartDato: LocalDate): LocalDate {
        if (fakturaStartDato <= dagensDato() || erSammeÅrOgKvartal(fakturaStartDato, dagensDato()) ||
            erNesteKvartalOgKvartalsbestillingHarKjørt(fakturaStartDato, dagensDato())
        ) {
            return dagensDato()
        }
        val førstMånedIKvartal = fakturaStartDato.month.firstMonthOfQuarter()
        return fakturaStartDato.withMonth(førstMånedIKvartal.value).minusMonths(1).withDayOfMonth(19)
    }

    private fun erSammeÅrOgKvartal(datoA: LocalDate, datoB: LocalDate): Boolean {
        return datoA[IsoFields.QUARTER_OF_YEAR] == datoB[IsoFields.QUARTER_OF_YEAR]
                && datoA.year == datoB.year
    }

    private fun tilFakturaTemp(fakturaLinjer: List<FakturaLinje>): Faktura {
        return Faktura(null, referanseNr = ULID.randomULID(), datoBestilt = dagensDato(), sistOppdatert = dagensDato(), fakturaLinje = fakturaLinjer.sortedByDescending { it.periodeFra })
    }

    protected fun dagensDato(): LocalDate = LocalDate.now()

}
