package no.nav.faktureringskomponenten.service

import io.getunleash.Unleash
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.threeten.extra.LocalDateRange
import ulid.ULID
import java.time.LocalDate

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
        fakturaseriePerioder: List<FakturaseriePeriode>,
        intervall: FakturaserieIntervall
    ): List<Faktura> {
        val dagensDato = dagensDato()

        val (historiskePerioder, fremtidigePerioder) = periodisering.partition { (startDato, _) ->
            !startDato.isAfter(dagensDato)
        }

        return lagFakturaForHistoriskePerioder(historiskePerioder, fakturaseriePerioder, intervall) +
            lagFremtidigeFakturaer(fremtidigePerioder, fakturaseriePerioder, intervall)
    }

    /**
     * Lager fakturaer for historiske perioder (perioder som har forfalt).
     * Historiske perioder blir gruppert per år og det lages én faktura per år.
     */
    private fun lagFakturaForHistoriskePerioder(
        historiskePerioder: List<Pair<LocalDate, LocalDate>>,
        fakturaseriePerioder: List<FakturaseriePeriode>,
        intervall: FakturaserieIntervall
    ): List<Faktura> {
        if (historiskePerioder.isEmpty()) return emptyList()
        val sluttDatoForHelePerioden = historiskePerioder.maxOf { it.second }
        return historiskePerioder
            // Grupper perioder per år
            .groupBy { (_, sluttDato) -> sluttDato.year }
            // Filtrer bort år som ikke har noen fakturaPerioder (f.eks. hvis det er opphold i faktureringen)
            .filterÅrMedFakturaPerioder(fakturaseriePerioder)
            // Lag én faktura per år med alle perioder samlet
            .map { (_, perioderForÅr) ->
                // Lager en fakturalinje for hver periode i året
                val fakturaLinjer = perioderForÅr.flatMap { (periodeStart, periodeSlutt) ->
                    lagFakturaLinjerForPeriode(
                        periodeStart,
                        periodeSlutt,
                        fakturaseriePerioder,
                        sluttDatoForHelePerioden
                    )
                }

                tilFaktura(
                    perioderForÅr.minOf { it.first },
                    fakturaLinjer.sortedByDescending { it.periodeFra },
                    intervall
                )
            }
            // Fjern eventuelle fakturaer som ikke inneholder noen linjer
            .filter { it.fakturaLinje.isNotEmpty() }
    }

    /**
     * Lager fakturaer for fremtidige perioder.
     * Hver periode i @param fremtidigePerioder får sin egen faktura hvis den overlapper med en fakturaseriePeriode.
     */
    private fun lagFremtidigeFakturaer(
        fremtidigePerioder: List<Pair<LocalDate, LocalDate>>,
        fakturaseriePerioder: List<FakturaseriePeriode>,
        intervall: FakturaserieIntervall
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
            .map { (start, slutt) -> lagFakturaForPeriode(start, slutt, fakturaseriePerioder, sluttDatoForHelePerioden, intervall) }
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

    private fun lagFakturaForPeriode(
        periodeStart: LocalDate,
        periodeSlutt: LocalDate,
        fakturaseriePerioder: List<FakturaseriePeriode>,
        sluttDatoForHelePerioden: LocalDate,
        intervall: FakturaserieIntervall
    ): Faktura {
        val fakturaLinjer = lagFakturaLinjerForPeriode(
            periodeStart,
            periodeSlutt,
            fakturaseriePerioder,
            sluttDatoForHelePerioden
        )
        return tilFaktura(periodeStart, fakturaLinjer, intervall)
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

    private fun tilFaktura(fakturaStartDato: LocalDate, fakturaLinjer: List<FakturaLinje>, intervall: FakturaserieIntervall): Faktura {
        val bestillingsdato = utledBestillingsdato(fakturaStartDato, intervall)

        return Faktura(
            null,
            referanseNr = ULID.randomULID(),
            datoBestilt = bestillingsdato,
            fakturaLinje = fakturaLinjer.sortedByDescending { it.periodeFra })
    }

    private fun utledBestillingsdato(fakturaStartDato: LocalDate, intervall: FakturaserieIntervall): LocalDate {
        if (unleash.isEnabled("melosys.faktureringskomponent.send_faktura_instant") && naisClusterName == NAIS_CLUSTER_NAME_DEV) {
            return dagensDato()
        }

        // Hvis fakturaen starter i fortiden eller i dag
        if (fakturaStartDato <= dagensDato()) {
            return dagensDato().plusDays(forsteFakturaOffsettMedDager)
        }

        // Hvis bestillingsdato for perioden har kjørt
        if (harBestillingsdatoKjørt(fakturaStartDato, intervall)) {
            return dagensDato().plusDays(forsteFakturaOffsettMedDager)
        }

        // Planlegg for den 19. i måneden før periodestart
        return fakturaStartDato.withDayOfMonth(1).minusMonths(1).withDayOfMonth(19)
    }

    private fun harBestillingsdatoKjørt(fakturaStartDato: LocalDate, intervall: FakturaserieIntervall): Boolean {
        val førsteIPerioden = when(intervall) {
            FakturaserieIntervall.MANEDLIG -> fakturaStartDato.withDayOfMonth(1)
            FakturaserieIntervall.KVARTAL -> fakturaStartDato.withMonth(fakturaStartDato.month.firstMonthOfQuarter().value).withDayOfMonth(1)
            FakturaserieIntervall.SINGEL -> throw IllegalArgumentException("Singelintervall er ikke støttet")
        }

        val bestillingsDato = førsteIPerioden.minusMonths(1).withDayOfMonth(19)
        return dagensDato() >= bestillingsDato
    }

    protected fun dagensDato(): LocalDate = LocalDate.now()

    companion object {
        private const val NAIS_CLUSTER_NAME_DEV = "dev-gcp"
    }
}
