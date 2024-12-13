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
            startDato.isBefore(dagensDato)
        }

        return lagFakturaerForHistoriskePerioder(historiskePerioder, fakturaseriePerioder, intervall) +
            lagFremtidigeFakturaer(fremtidigePerioder, fakturaseriePerioder, intervall)
    }

    /**
     * Lager fakturaer for historiske perioder (perioder som har forfalt).
     * Historiske perioder blir gruppert per år og det lages én faktura per år.
     */
    private fun lagFakturaerForHistoriskePerioder(
        historiskePerioder: List<Pair<LocalDate, LocalDate>>,
        fakturaseriePerioder: List<FakturaseriePeriode>,
        intervall: FakturaserieIntervall
    ): List<Faktura> {
        if (historiskePerioder.isEmpty()) return emptyList()
        return historiskePerioder
            // Grupper perioder per år siden det lages én faktura per år.
            .groupBy { (_, sluttDato) -> sluttDato.year }
            // Filtrer bort år uten fakturering, hvis det er opphold i faktureringen
            .filterÅrMedFakturaPerioder(fakturaseriePerioder)
            // Lager en fakturalinje for hver periode i året
            .mapValues { (_, perioderForÅr) ->
                perioderForÅr.flatMap { (periodeStart, periodeSlutt) ->
                    lagFakturaLinjerForPeriode(
                        periodeStart,
                        periodeSlutt,
                        fakturaseriePerioder
                    )
                }
            }
            // Lag én faktura per år med alle perioder samlet
            .mapNotNull { (_, fakturaLinjer) ->
                if (fakturaLinjer.isEmpty()) {
                    null // Lag kun fakturaer som inneholder linjer
                } else {
                    tilFaktura(
                        fakturaLinjer.sortedByDescending { it.periodeFra },
                        intervall
                    )
                }
            }
    }

    /**
     * Lager fakturaer for fremtidige perioder.
     * Hver periode i @param fremtidigePerioder får sin egen faktura hvis den overlapper med en FakturaseriePeriode.
     */
    private fun lagFremtidigeFakturaer(
        fremtidigePerioder: List<Pair<LocalDate, LocalDate>>,
        fakturaseriePerioder: List<FakturaseriePeriode>,
        intervall: FakturaserieIntervall
    ): List<Faktura> {
        if (fremtidigePerioder.isEmpty()) return emptyList()
        return fremtidigePerioder
            // Filtrerer fremtidige perioder som overlapper med faktureringsgrunnlaget
            .filter { (start, slutt) ->
                val periodeRange = LocalDateRange.ofClosed(start, slutt)
                fakturaseriePerioder.any { periode ->
                    LocalDateRange.ofClosed(periode.startDato, periode.sluttDato).overlaps(periodeRange)
                }
            }
            // Lag en Map med periode -> fakturalinjer
            .associateWith { lagFakturaLinjerForPeriode(it.first, it.second, fakturaseriePerioder) }
            // Lag en faktura per periode hvis det finnes fakturalinjer
            .mapNotNull { (_, fakturaLinjer) ->
                if (fakturaLinjer.isEmpty()) {
                    null
                } else {
                    tilFaktura(
                        fakturaLinjer.sortedByDescending { it.periodeFra },
                        intervall
                    )
                }
            }
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
        intervall: FakturaserieIntervall
    ): Faktura {
        val fakturaLinjer = lagFakturaLinjerForPeriode(
            periodeStart,
            periodeSlutt,
            fakturaseriePerioder
        )
        return tilFaktura(fakturaLinjer, intervall)
    }

    private fun lagFakturaLinjerForPeriode(
        periodeStart: LocalDate,
        periodeSlutt: LocalDate,
        fakturaseriePerioder: List<FakturaseriePeriode>
    ): List<FakturaLinje> = fakturalinjeGenerator.lagFakturaLinjer(
        perioder = fakturaseriePerioder,
        faktureringFra = periodeStart,
        faktureringTil = periodeSlutt
    )


    private fun tilFaktura(fakturaLinjer: List<FakturaLinje>, intervall: FakturaserieIntervall): Faktura {
        val bestillingsdato = utledBestillingsdato(fakturaLinjer.minOf { it.periodeFra }, intervall)

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

        // Planlegg for den 19. i måneden før periodestart hvis intervall er MÅNEDLIG. Ellers 19. i måneden før kvartalet dersom intervall er KVARTAL
        return when (intervall) {
            FakturaserieIntervall.MANEDLIG -> fakturaStartDato.withDayOfMonth(1).minusMonths(1).withDayOfMonth(19)
            FakturaserieIntervall.KVARTAL -> fakturaStartDato.withMonth(fakturaStartDato.month.firstMonthOfQuarter().value).withDayOfMonth(1).minusMonths(1).withDayOfMonth(19)
            FakturaserieIntervall.SINGEL -> throw IllegalArgumentException("Singelintervall er ikke støttet")
        }
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
