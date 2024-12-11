package no.nav.faktureringskomponenten.service

import io.getunleash.Unleash
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.threeten.extra.LocalDateRange
import ulid.ULID
import java.time.LocalDate
import java.time.Month
import java.time.temporal.IsoFields

@Component
class FakturaGenerator(
    private val fakturalinjeGenerator: FakturaLinjeGenerator,
    private val unleash: Unleash,
    @Value("\${faktura.forste-faktura-offsett-dager}") private val forsteFakturaOffsettMedDager: Long
) {
    @Value("\${NAIS_CLUSTER_NAME}")
    private lateinit var naisClusterName: String

    /**
     * Genererer fakturaer basert på gitte perioder og faktureringsgrunnlag.
     * Historiske perioder (til og med dagens dato) grupperes per år,
     * mens fremtidige perioder faktureres per periode definert i 'periodisering'.
     *
     * @param periodisering Liste av perioder som definerer fakturaoppdelingen (typisk kvartalsvis, men kan være månedlig)
     * @param fakturaseriePerioder Liste av perioder som definerer faktureringsgrunnlaget og enhetspris per måned
     * @return Liste av fakturaer gruppert etter historiske (årlig) og fremtidige (per periode) perioder
     */
    fun lagFakturaerFor(
        periodisering: List<Pair<LocalDate, LocalDate>>,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<Faktura> {
        val dagensDato = dagensDato()

        val (historiskePerioder, fremtidigePerioder) = periodisering.partition { (_, sluttDato) ->
            !sluttDato.isAfter(dagensDato)
        }

        return lagFakturaForHistoriskePerioder(historiskePerioder, fakturaseriePerioder) +
            lagFremtidigeFakturaer(fremtidigePerioder, fakturaseriePerioder)
    }

    /**
     * Lager fakturaer for historiske perioder (perioder som har forfalt).
     * Historiske perioder blir gruppert per år og det lages én faktura per år.
     */
    private fun lagFakturaForHistoriskePerioder(
        historiskePerioder: List<Pair<LocalDate, LocalDate>>,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<Faktura> {
        if (historiskePerioder.isEmpty()) return emptyList()
        val sluttDatoForHelePerioden = historiskePerioder.maxOf { it.second }
        return historiskePerioder
            // Grupper perioder per år
            .groupBy { (_, sluttDato) -> sluttDato.year }
            // Filtrer bort år som ikke har noen fakturaPerioder (f.eks. hvis det er opphold i faktureringen)
            .filterÅrMedFakturaPerioder(fakturaseriePerioder)
            // Lag én faktura per år med alle perioder samlet
            .map { (_, perioderForÅr) -> perioderForÅr.tilFaktura(fakturaseriePerioder, sluttDatoForHelePerioden) }
            // Fjern eventuelle fakturaer som ikke inneholder noen linjer
            .filter { it.fakturaLinje.isNotEmpty() }
    }

    /**
     * Lager fakturaer for fremtidige perioder.
     * Hver periode i @param fremtidigePerioder får sin egen faktura hvis den overlapper med en fakturaseriePeriode.
     */
    private fun lagFremtidigeFakturaer(
        fremtidigePerioder: List<Pair<LocalDate, LocalDate>>,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<Faktura> {
        if (fremtidigePerioder.isEmpty()) return emptyList()
        val sluttDatoForHelePerioden = fremtidigePerioder.maxOf { it.second }
        return fremtidigePerioder
            .filter { (start, slutt) ->
                val periodeRange = LocalDateRange.ofClosed(start, slutt)
                fakturaseriePerioder.any { periode ->
                    LocalDateRange.ofClosed(periode.startDato, periode.sluttDato).overlaps(periodeRange)
                }
            }
            .map { (start, slutt) -> lagFakturaForPeriode(start, slutt, fakturaseriePerioder, sluttDatoForHelePerioden) }
            .filter { it.fakturaLinje.isNotEmpty() }
    }

    /**
     * Filtrerer bort år som ikke har noen overlappende fakturaperioder.
     * Dette er nødvendig for å håndtere opphold i faktureringsperioder,
     * f.eks. hvis det ikke skal faktureres for et helt år.
     */
    private fun Map<Int, List<Pair<LocalDate, LocalDate>>>.filterÅrMedFakturaPerioder(
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): Map<Int, List<Pair<LocalDate, LocalDate>>> {
        return filter { (år, _) ->
            val årRange = LocalDateRange.ofClosed(
                LocalDate.of(år, 1, 1),
                LocalDate.of(år, 12, 31)
            )
            fakturaseriePerioder.any { periode ->
                LocalDateRange.ofClosed(periode.startDato, periode.sluttDato).overlaps(årRange)
            }
        }
    }

    /**
     * Konverterer en liste av perioder til én faktura.
     * Start- og sluttdato for fakturaen blir henholdsvis den tidligste og seneste datoen fra periodene.
     */
    private fun List<Pair<LocalDate, LocalDate>>.tilFaktura(
        fakturaseriePerioder: List<FakturaseriePeriode>,
        sluttDatoForHelePerioden: LocalDate
    ): Faktura {
        val periodeStart = minOf { it.first }
        val periodeSlutt = maxOf { it.second }
        return lagFakturaForPeriode(periodeStart, periodeSlutt, fakturaseriePerioder, sluttDatoForHelePerioden)
    }

    private fun lagFakturaForPeriode(
        periodeStart: LocalDate,
        periodeSlutt: LocalDate,
        fakturaseriePerioder: List<FakturaseriePeriode>,
        sluttDatoForHelePerioden: LocalDate
    ): Faktura {
        val fakturaLinjer = lagFakturaLinjerForPeriode(
            periodeStart,
            periodeSlutt,
            fakturaseriePerioder,
            sluttDatoForHelePerioden
        )
        return tilFaktura(periodeStart, fakturaLinjer)
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
