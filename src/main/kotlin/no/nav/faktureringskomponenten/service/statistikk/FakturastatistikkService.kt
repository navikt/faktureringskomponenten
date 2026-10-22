package no.nav.faktureringskomponenten.service.statistikk

import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import no.nav.faktureringskomponenten.domain.repositories.BestiltGruppe
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieGruppe
import no.nav.faktureringskomponenten.domain.repositories.FakturastatistikkRepository
import no.nav.faktureringskomponenten.service.integration.kafka.EksternFakturaStatusConsumer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Service
class FakturastatistikkService(
    private val repository: FakturastatistikkRepository,
    private val eksternFakturaStatusConsumer: EksternFakturaStatusConsumer,
) {

    /**
     * Repeatable read gir ett øyeblikksbilde for alle spørringene, så tallene kan avstemmes mot hverandre.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun hentStatistikk(
        fraOgMed: LocalDate?,
        tilOgMed: LocalDate?,
        naa: Instant = Instant.now(),
    ): FakturastatistikkDto {
        require(fraOgMed == null || tilOgMed == null || !fraOgMed.isAfter(tilOgMed)) {
            "fraOgMed ($fraOgMed) kan ikke være etter tilOgMed ($tilOgMed)"
        }
        val idag = LocalDate.ofInstant(naa, SONE)
        val fra = fraOgMed ?: LocalDate.of(1900, 1, 1)
        val til = tilOgMed ?: LocalDate.of(3000, 12, 31)

        val naatilstand = repository.naatilstand(idag, naa)
        val mottak = repository.mottak()
        val feilPerMelding = repository.feilPerMelding(MAKS_FEILMELDINGER)

        return FakturastatistikkDto(
            tidspunkt = naa,
            periodeFraOgMed = fraOgMed,
            periodeTilOgMed = tilOgMed,
            drift = DriftDto(
                forfaltIkkeBestilt = ForfaltIkkeBestiltDto(
                    antall = naatilstand.forfalt,
                    eldsteBestillingsdato = naatilstand.eldsteForfalt,
                    eldste = if (naatilstand.forfalt > 0) repository.eldsteForfalte(idag, MAKS_OPPFOELGING) else emptyList(),
                ),
                venterPaaKvittering = VenterPaaKvitteringDto(
                    antall = naatilstand.venter,
                    under3Dager = naatilstand.venterUnder3Dager,
                    mellom3Og7Dager = naatilstand.venter3Til7Dager,
                    mellom7Og30Dager = naatilstand.venter7Til30Dager,
                    over30Dager = naatilstand.venterOver30Dager,
                    eldsteSendt = naatilstand.eldsteVenter,
                    eldsteOver7Dager = repository.eldsteUtenKvittering(naa.minus(Duration.ofDays(7)), MAKS_OPPFOELGING),
                    antallKoblingsfakturaerHoldtUtenfor = naatilstand.koblingsfakturaer,
                ),
                feil = FeilDto(
                    antall = naatilstand.feil,
                    eldsteFeilDato = feilPerMelding.mapNotNull { it.eldste }.minOrNull(),
                    perFeilmelding = feilPerMelding.map { FeilmeldingDto(it.melding, it.antall, it.eldste) },
                ),
                mottak = MottakDto(
                    lytterKjoerer = runCatching { eksternFakturaStatusConsumer.eksternFakturaStatusListenerContainer().isRunning }
                        .getOrNull(),
                    antallMottaksfeil = mottak.antallMottaksfeil,
                    sisteMottaksfeil = mottak.sisteMottaksfeil,
                    sisteStatusmelding = mottak.sisteStatusmelding,
                    antallManglendeInnbetalingIkkeMeldt = mottak.manglendeInnbetalingIkkeMeldt,
                ),
                antallManglendeInnbetaling = naatilstand.manglendeInnbetaling,
                muligDobbeltfakturering = repository.muligDobbeltfakturering().map { DobbeltfaktureringDto(it) },
            ),
            bestilt = lagBestilt(repository.bestiltGruppert(fra, til), repository.antallUnikePersonerBestilt(fra, til)),
            planlagt = PlanlagtDto(
                antall = naatilstand.planlagt,
                sumBeloep = naatilstand.planlagtBeloep,
                neste30Dager = naatilstand.planlagtNeste30Dager,
                neste30DagerBeloep = naatilstand.planlagtNeste30DagerBeloep,
                antallFakturaserier = naatilstand.serierMedPlanlagte,
                antallUnikePersoner = naatilstand.personerMedPlanlagte,
            ),
            fakturaserier = lagFakturaserier(fra, til),
            perMaaned = lagPerMaaned(YearMonth.from(idag)),
        )
    }

    private fun lagBestilt(grupper: List<BestiltGruppe>, antallUnikePersoner: Long): BestiltDto {
        val manglende = grupper.filter { it.status == FakturaStatus.MANGLENDE_INNBETALING.name }
        val antallManglende = manglende.sumOf { it.antall }
        val ikkeBetalt = manglende.sumOf { it.ikkeBetalt }
        val delvisBetalt = manglende.sumOf { it.delvisBetalt }
        val sumFakturert = grupper.sumOf { it.sumDebet }
        val sumKreditert = grupper.sumOf { it.sumKredit }

        return BestiltDto(
            antall = grupper.sumOf { it.antall },
            sumFakturert = sumFakturert,
            sumKreditert = sumKreditert,
            sumNetto = sumFakturert + sumKreditert,
            antallUnikePersoner = antallUnikePersoner,
            perStatus = SENDTE_STATUSER.associateWith { status -> grupper.antallDer { it.status == status.name } },
            perType = Fakturatype.entries.associateWith { type -> grupper.antallDer { it.type == type.name } },
            perInnbetalingstype = Innbetalingstype.entries.associateWith { type ->
                grupper.antallDer { it.innbetalingstype == type.name }
            },
            manglendeInnbetaling = ManglendeInnbetalingDto(
                antall = antallManglende,
                ikkeBetalt = ikkeBetalt,
                delvisBetalt = delvisBetalt,
                utenDetaljer = antallManglende - ikkeBetalt - delvisBetalt,
                sumUbetalt = manglende.sumOf { it.sumUbetalt },
            ),
        )
    }

    private fun lagFakturaserier(fra: LocalDate, til: LocalDate): FakturaserieStatistikkDto {
        val fraTidspunkt = startAvDag(fra)
        val tilTidspunkt = startAvDag(til.plusDays(1))
        val grupper = repository.fakturaserierGruppert(fraTidspunkt, tilTidspunkt)

        return FakturaserieStatistikkDto(
            antall = grupper.sumOf { it.antall },
            antallUnikePersoner = repository.antallUnikePersonerFakturaserier(fraTidspunkt, tilTidspunkt),
            perStatus = grupper.fordeling(FakturaserieStatus.entries) { it.status },
            perInnbetalingstype = grupper.fordeling(Innbetalingstype.entries) { it.innbetalingstype },
            perIntervall = grupper.fordeling(FakturaserieIntervall.entries) { it.intervall },
            perKilde = grupper.fordeling(Fakturakilde.entries) { it.kilde },
            perHendelse = grupper.fordeling(Fakturaseriehendelse.entries) { it.hendelse },
            perFullmektig = grupper.fordeling(Fullmektigtype.entries) { it.fullmektig },
        )
    }

    private fun lagPerMaaned(innevaerende: YearMonth): List<MaanedDto> {
        val foersteMaaned = innevaerende.minusMonths(MAANEDER_TILBAKE)
        val etterSisteMaaned = innevaerende.plusMonths(MAANEDER_FRAM + 1)
        val fakturaer = repository.fakturaerPerMaaned(foersteMaaned.atDay(1), etterSisteMaaned.atDay(1))
            .associateBy { it.maaned }
        val serier = repository.fakturaserierPerMaaned(
            startAvDag(foersteMaaned.atDay(1)),
            startAvDag(etterSisteMaaned.atDay(1))
        )

        return generateSequence(foersteMaaned) { it.plusMonths(1) }
            .takeWhile { it.isBefore(etterSisteMaaned) }
            .map { maaned ->
                val nokkel = maaned.toString()
                val f = fakturaer[nokkel]
                MaanedDto(
                    maaned = nokkel,
                    antallBestilt = f?.antallBestilt ?: 0,
                    beloepBestilt = f?.beloepBestilt ?: BigDecimal.ZERO,
                    antallPlanlagt = f?.antallPlanlagt ?: 0,
                    beloepPlanlagt = f?.beloepPlanlagt ?: BigDecimal.ZERO,
                    antallFakturaserierOpprettet = serier[nokkel] ?: 0,
                )
            }
            .toList()
    }

    private fun List<BestiltGruppe>.antallDer(predikat: (BestiltGruppe) -> Boolean) = filter(predikat).sumOf { it.antall }

    private fun <E : Enum<E>> List<FakturaserieGruppe>.fordeling(
        verdier: List<E>,
        nokkel: (FakturaserieGruppe) -> String
    ): Map<E, Long> = verdier.associateWith { verdi -> filter { nokkel(it) == verdi.name }.sumOf { it.antall } }

    private fun startAvDag(dato: LocalDate): Instant = dato.atStartOfDay(SONE).toInstant()

    companion object {
        const val MAKS_OPPFOELGING = 20
        const val MAKS_FEILMELDINGER = 10
        private const val MAANEDER_TILBAKE = 11L
        private const val MAANEDER_FRAM = 12L
        private val SONE: ZoneId = ZoneId.of("Europe/Oslo")

        /** Statusene en faktura kan ha etter at den er sendt til OEBS. */
        val SENDTE_STATUSER = listOf(
            FakturaStatus.BESTILT,
            FakturaStatus.INNE_I_OEBS,
            FakturaStatus.FEIL,
            FakturaStatus.MANGLENDE_INNBETALING,
        )
    }
}
