package no.nav.faktureringskomponenten.service

import io.getunleash.Unleash
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ulid.ULID
import java.time.LocalDate
import java.time.Month
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

@Component
class FakturaGenerator(
    private val fakturalinjeGenerator: FakturaLinjeGenerator,
    private val unleash: Unleash,
    @Value("\${faktura.forste-faktura-offsett-dager}") private val forsteFakturaOffsettMedDager: Long
) {
    @Value("\${NAIS_CLUSTER_NAME}")
    private lateinit var naisClusterName: String

    // FIXME: Dette er en midlertidig løsning for å fikse https://jira.adeo.no/browse/MELOSYS-6957
    fun lagFaktura(
        startDato: LocalDate,
        sluttDato: LocalDate,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): Faktura {
        val fakturaLinjer = lagFakturaLinjerForPeriode(startDato, sluttDato, fakturaseriePerioder, sluttDato)

        return tilFaktura(startDato, fakturaLinjer)
    }

    fun lagFakturaerFor(
        startDatoForHelePerioden: LocalDate,
        sluttDatoForHelePerioden: LocalDate,
        fakturaseriePerioder: List<FakturaseriePeriode>,
        faktureringsintervall: FakturaserieIntervall
    ): List<Faktura> {
        val periodisering = genererPeriodisering(startDatoForHelePerioden, sluttDatoForHelePerioden, faktureringsintervall)

        return lagFakturaerFor(periodisering, fakturaseriePerioder)
    }

    private fun lagFakturaerFor(
        periodisering: List<Pair<LocalDate, LocalDate>>,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<Faktura> {
        // val sluttDatoForPeriodisering = periodisering.last().second
        return periodisering.fold(
            initial = emptyList<FakturaLinje>() to emptyList<Faktura>()
        ) { (gjeldendeLinjer, akkumulerteFakturaer), (periodeStart, periodeSlutt) ->
            val nyeFakturaLinjer = lagFakturaLinjerForPeriode(
                periodeStart, periodeSlutt, fakturaseriePerioder, periodisering.last().second
            )
            val oppdaterteFakturaLinjer = gjeldendeLinjer + nyeFakturaLinjer

            if ((periodeSlutt >= dagensDato() || erSisteDagIÅret(periodeSlutt)) && oppdaterteFakturaLinjer.isNotEmpty()) {
                val nyFaktura = tilFaktura(periodeStart, oppdaterteFakturaLinjer)
                emptyList<FakturaLinje>() to akkumulerteFakturaer + nyFaktura
            } else if (periodeSlutt == periodisering.last().second && oppdaterteFakturaLinjer.isNotEmpty()) {
                val sisteFaktura = tilFaktura(oppdaterteFakturaLinjer.minOf { it.periodeFra }, oppdaterteFakturaLinjer)
                emptyList<FakturaLinje>() to akkumulerteFakturaer + sisteFaktura
            } else {
                oppdaterteFakturaLinjer to akkumulerteFakturaer
            }
        }.second
    }

    private fun genererPeriodisering(
        startDatoForPerioden: LocalDate,
        sluttDatoForPerioden: LocalDate,
        faktureringsintervall: FakturaserieIntervall
    ): List<Pair<LocalDate, LocalDate>> = generateSequence(startDatoForPerioden) { startDato ->
        sluttDatoFor(startDato, faktureringsintervall).plusDays(1)
    }.takeWhile { it <= sluttDatoForPerioden }
        .map { startDato ->
            val sluttDato = minOf(sluttDatoFor(startDato, faktureringsintervall), sluttDatoForPerioden)
            startDato to sluttDato
        }.toList()

    private fun sluttDatoFor(startDato: LocalDate, intervall: FakturaserieIntervall): LocalDate {
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

    private fun erSisteDagIÅret(dato: LocalDate): Boolean = dato.month == Month.DECEMBER && dato.dayOfMonth == 31

    private fun tilFaktura(fakturaStartDato: LocalDate, fakturaLinjer: List<FakturaLinje>): Faktura {
        val bestillingsdato = utledBestillingsdato(fakturaStartDato)

        return Faktura(
            null,
            referanseNr = ULID.randomULID(),
            datoBestilt = bestillingsdato,
            fakturaLinje = fakturaLinjer.sortedByDescending { it.periodeFra })
    }

    private fun erNesteKvartalOgKvartalsbestillingHarKjørt(
        fakturaStartDato: LocalDate,
        dagensDato: LocalDate,
    ): Boolean {
        val erNesteKvartal = dagensDato < fakturaStartDato && dagensDato[IsoFields.QUARTER_OF_YEAR]
            .plus(1) % 4 == fakturaStartDato[IsoFields.QUARTER_OF_YEAR] % 4
        val sisteMånedIDagensKvartal = dagensDato.month.firstMonthOfQuarter().plus(2)
        val kvartalsBestillingHarKjørt =
            dagensDato > LocalDate.now().withMonth(sisteMånedIDagensKvartal.value).withDayOfMonth(19)
        val datoErEtter19Desember = dagensDato >= LocalDate.now().withMonth(12).withDayOfMonth(19)
        val fakturaStartDatoErÅretEtterOgFørsteKvartal =
            fakturaStartDato.year == dagensDato.plusYears(1).year && fakturaStartDato.month.value <= 3
        return erNesteKvartal && kvartalsBestillingHarKjørt && (fakturaStartDato.year == dagensDato.year || (datoErEtter19Desember && fakturaStartDatoErÅretEtterOgFørsteKvartal))
    }

    private fun utledBestillingsdato(fakturaStartDato: LocalDate): LocalDate {
        if (unleash.isEnabled("melosys.faktureringskomponent.send_faktura_instant") && naisClusterName == NAIS_CLUSTER_NAME_DEV) {
            return dagensDato()
        }

        if (fakturaStartDato <= dagensDato() || erInneværendeÅrOgKvartal(fakturaStartDato, dagensDato()) ||
            erNesteKvartalOgKvartalsbestillingHarKjørt(fakturaStartDato, dagensDato())
        ) {
            return dagensDato().plusDays(forsteFakturaOffsettMedDager)
        }
        val førstMånedIKvartal = fakturaStartDato.month.firstMonthOfQuarter()
        return fakturaStartDato.withMonth(førstMånedIKvartal.value).minusMonths(1).withDayOfMonth(19)
    }

    private fun erInneværendeÅrOgKvartal(datoA: LocalDate, datoB: LocalDate): Boolean {
        return datoA[IsoFields.QUARTER_OF_YEAR] == datoB[IsoFields.QUARTER_OF_YEAR]
                && datoA.year == datoB.year
    }

    protected fun dagensDato(): LocalDate = LocalDate.now()

    companion object {
        private const val NAIS_CLUSTER_NAME_DEV = "dev-gcp"
    }
}
