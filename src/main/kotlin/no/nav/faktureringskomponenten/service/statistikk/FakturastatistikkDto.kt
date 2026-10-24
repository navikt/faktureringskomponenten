package no.nav.faktureringskomponenten.service.statistikk

import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import no.nav.faktureringskomponenten.domain.repositories.FakturaOppfoelging
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/**
 * Fakturastatistikk for melosys-console. Kun aggregerte tall og referanser (ULID-er) – ingen
 * fødselsnumre, navn eller organisasjonsnumre.
 *
 * To slags tall:
 * - Nåtilstand ([drift], [planlagt], [perMaaned]) påvirkes ikke av periodefilteret.
 * - [bestilt] filtreres på fakturaens bestillingsdato, og [fakturaserier] på når serien ble opprettet.
 */
data class FakturastatistikkDto(
    val tidspunkt: Instant,
    /** Periodefilteret som ble brukt. Null = ingen grense. */
    val periodeFraOgMed: LocalDate?,
    val periodeTilOgMed: LocalDate?,
    val drift: DriftDto,
    val bestilt: BestiltDto,
    val planlagt: PlanlagtDto,
    val fakturaserier: FakturaserieStatistikkDto,
    /** Fakturaer per bestillingsmåned, fra 11 måneder tilbake til 12 måneder fram. */
    val perMaaned: List<MaanedDto>,
)

/** Nåtilstand for flyten mot OEBS. Alt her er tall som bør være 0 eller lave når alt er vel. */
data class DriftDto(
    val forfaltIkkeBestilt: ForfaltIkkeBestiltDto,
    val venterPaaKvittering: VenterPaaKvitteringDto,
    val feil: FeilDto,
    val mottak: MottakDto,
    /** Fakturaer som nå står i MANGLENDE_INNBETALING, uansett bestillingsdato. */
    val antallManglendeInnbetaling: Long,
    /**
     * Personer med planlagte trygdeavgiftsfakturaer i flere fakturaserier med overlappende periode.
     * Ett element per person (uten fødselsnummer). Kan være legitimt, men er verdt en kikk.
     */
    val muligDobbeltfakturering: List<DobbeltfaktureringDto>,
)

/**
 * Planlagte fakturaer med bestillingsdato før i dag. Bestillingsjobben skal ha plukket dem opp, så
 * alt over 0 betyr at jobben ikke har kjørt eller har feilet.
 */
data class ForfaltIkkeBestiltDto(
    val antall: Long,
    val eldsteBestillingsdato: LocalDate?,
    /** De eldste, maks [FakturastatistikkService.MAKS_OPPFOELGING]. */
    val eldste: List<FakturaOppfoelging>,
)

/**
 * Fakturaer sendt til OEBS (status BESTILT) som ennå ikke har fått kvittering (INNE_I_OEBS eller FEIL).
 * Alder regnes fra da fakturaen sist ble endret, i praksis da den ble (om)bestilt. Bøttene summerer
 * til [antall].
 */
data class VenterPaaKvitteringDto(
    val antall: Long,
    val under3Dager: Long,
    val mellom3Og7Dager: Long,
    val mellom7Og30Dager: Long,
    val over30Dager: Long,
    val eldsteSendt: Instant?,
    /** De eldste som har ventet mer enn 7 dager, maks [FakturastatistikkService.MAKS_OPPFOELGING]. */
    val eldsteOver7Dager: List<FakturaOppfoelging>,
    /**
     * Avregningsfakturaer med beløp 0 står i BESTILT uten å være sendt til OEBS – de finnes bare for
     * å koble en tidligere faktura til avregningen. De er holdt utenfor alle tall over.
     */
    val antallKoblingsfakturaerHoldtUtenfor: Long,
)

data class FeilDto(
    /** Fakturaer som nå står i FEIL. */
    val antall: Long,
    /** Tidligste dato OEBS meldte feil, blant fakturaene som står i FEIL. */
    val eldsteFeilDato: LocalDate?,
    /** Gruppert på siste feilmelding fra OEBS, flest først. Maks 10 grupper. */
    val perFeilmelding: List<FeilmeldingDto>,
)

data class FeilmeldingDto(
    val melding: String,
    val antall: Long,
    val eldsteFeilDato: LocalDate?,
)

/** Mottak av statusmeldinger fra OEBS (Kafka). */
data class MottakDto(
    /**
     * Om Kafka-lytteren for statusmeldinger kjører i podden som svarte. Null hvis det ikke kunne avgjøres.
     * Lytteren stopper ved første melding den ikke kan behandle, og må startes manuelt.
     */
    val lytterKjoerer: Boolean?,
    /** Lagrede meldinger som stoppet lytteren, totalt (ryddes ikke automatisk). */
    val antallMottaksfeil: Long,
    val sisteMottaksfeil: Instant?,
    /** Sist vi lagret en statusmelding fra OEBS. */
    val sisteStatusmelding: Instant?,
    /** Meldinger om manglende innbetaling som ikke er videresendt til Melosys. Skal være 0. */
    val antallManglendeInnbetalingIkkeMeldt: Long,
)

data class DobbeltfaktureringDto(
    val fakturaserieReferanser: List<String>,
)

/**
 * Fakturaer sendt til OEBS med bestillingsdato i perioden. Koblingsfakturaer er holdt utenfor.
 * [perStatus], [perType] og [perInnbetalingstype] summerer hver for seg til [antall].
 */
data class BestiltDto(
    val antall: Long,
    /** Sum av fakturaer med positivt beløp. */
    val sumFakturert: BigDecimal,
    /** Sum av fakturaer med negativt beløp (negativt tall). */
    val sumKreditert: BigDecimal,
    val sumNetto: BigDecimal,
    val antallUnikePersoner: Long,
    /** Nåværende status for fakturaene som ble bestilt i perioden. */
    val perStatus: Map<FakturaStatus, Long>,
    val perType: Map<Fakturatype, Long>,
    val perInnbetalingstype: Map<Innbetalingstype, Long>,
    val manglendeInnbetaling: ManglendeInnbetalingDto,
)

/**
 * Fakturaer bestilt i perioden som nå står i MANGLENDE_INNBETALING. Fordelingen er hentet fra siste
 * melding om manglende innbetaling fra OEBS: [ikkeBetalt] + [delvisBetalt] + [utenDetaljer] = [antall].
 */
data class ManglendeInnbetalingDto(
    val antall: Long,
    val ikkeBetalt: Long,
    val delvisBetalt: Long,
    /** Står i MANGLENDE_INNBETALING uten at vi har lagret meldingen fra OEBS (f.eks. satt manuelt). */
    val utenDetaljer: Long,
    val sumUbetalt: BigDecimal,
)

/** Planlagte fakturaer (status OPPRETTET) som ikke er sendt ennå. Nåtilstand. */
data class PlanlagtDto(
    val antall: Long,
    val sumBeloep: BigDecimal,
    /** Med bestillingsdato fra og med i dag til 30 dager fram. */
    val neste30Dager: Long,
    val neste30DagerBeloep: BigDecimal,
    val antallFakturaserier: Long,
    val antallUnikePersoner: Long,
)

/**
 * Fakturaserier opprettet i perioden. Hver fordeling summerer til [antall].
 * NB: en endring i Melosys gir en ny serie som erstatter den gamle, så [antall] teller versjoner,
 * ikke saker – se [perHendelse].
 */
data class FakturaserieStatistikkDto(
    val antall: Long,
    val antallUnikePersoner: Long,
    val perStatus: Map<FakturaserieStatus, Long>,
    val perInnbetalingstype: Map<Innbetalingstype, Long>,
    val perIntervall: Map<FakturaserieIntervall, Long>,
    val perKilde: Map<Fakturakilde, Long>,
    val perHendelse: Map<Fakturaseriehendelse, Long>,
    val perFullmektig: Map<Fullmektigtype, Long>,
)

data class MaanedDto(
    /** «YYYY-MM» */
    val maaned: String,
    /** Fakturaer sendt til OEBS med bestillingsdato i måneden. */
    val antallBestilt: Long,
    val beloepBestilt: BigDecimal,
    /** Planlagte fakturaer med bestillingsdato i måneden. For tidligere måneder er dette forfalte. */
    val antallPlanlagt: Long,
    val beloepPlanlagt: BigDecimal,
    val antallFakturaserierOpprettet: Long,
)

enum class Fakturatype {
    /** Vanlig periodisk faktura. */
    ORDINAER,
    /** Avregning mot en tidligere bestilt faktura, positiv eller negativ. */
    AVREGNING,
    /** Negativt beløp: kreditnota ved kansellering, admin-kreditering eller årsavregning til gode. */
    KREDITERING
}

/** Flyten i Melosys som opprettet serien, utledet fra teksten til bruker (referanseBruker). */
enum class Fakturakilde {
    VEDTAK,
    EOS_PENSJONIST,
    SATSENDRING,
    AARSAVREGNING,
    ANNET
}

enum class Fakturaseriehendelse {
    /** Første serie for saken. */
    NY,
    /** Erstatter en tidligere serie (endret vedtak, satsendring o.l.). */
    ENDRING,
    /** Kreditnota-serie opprettet ved kansellering. */
    KANSELLERING
}

enum class Fullmektigtype {
    ORGANISASJON,
    PERSON,
    INGEN
}
