package no.nav.faktureringskomponenten.statistikk

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class StatistikkOyeblikksbildeDto(
    val versjon: Int,
    val beregnet: Instant,
    val varighetMs: Long,
    val idag: LocalDate,
    val kjoeringer: List<KjoeringDto>,
    val nesteKjoering: NesteKjoeringDto,
    val naa: NaatilstandDto,
    val meldingerOmManglendeInnbetaling: List<MeldingOmManglendeInnbetalingDto>,
    val planlagt: PlanlagtDto,
    val planlagtPerKvartal: List<PlanlagtKvartalDto>,
    val utenomPerKvartal: List<UtenomKvartalDto>,
    val perioder: List<PeriodeDto>,
    val fakturaserier: FakturaserierDto,
)

data class AntallBeloepDto(
    val antall: Long,
    val beloep: BigDecimal,
)

data class PlanlagtDto(
    val forfalt: AntallBeloepDto,
    val foerNesteKjoering: AntallBeloepDto,
    val iNesteKjoering: AntallBeloepDto,
    val senere: AntallBeloepDto,
    val totalt: AntallBeloepDto,
    val sisteBestillingsdato: LocalDate?,
    val serier: Long,
    val personer: Long,
)

data class PlanlagtKvartalDto(
    val kvartal: String,
    val antall: Long,
    val beloep: BigDecimal,
)

data class UtenomKvartalDto(
    val kvartal: String,
    val sendt: Long,
    val beloep: BigDecimal,
    val perType: Map<String, AntallBeloepDto>,
    val perKilde: Map<String, Long>,
    val feiletNoenGang: Long,
    val manglendeInnbetaling: Long,
)

data class PeriodeDto(
    val type: String,
    val noekkel: String,
    val sendt: Long,
    val fakturert: BigDecimal,
    val kreditert: BigDecimal,
    val netto: BigDecimal,
    val personer: Long,
    val perType: Map<String, AntallBeloepDto>,
    val perBestillingsmaate: Map<String, AntallBeloepDto>,
    val perStatus: Map<String, Long>,
    val feil: PeriodeFeilDto,
    val manglendeInnbetaling: PeriodeManglendeInnbetalingDto,
)

data class PeriodeFeilDto(
    val noenGang: Long,
    val loest: Long,
    val uloest: Long,
)

/**
 * Gruppene summerer til [antall]: ikke betalt når ubetalt ≥ fakturabeløp, delvis betalt når ubetalt < fakturabeløp
 * (også 0), og uten detaljer når ett av beløpene mangler i siste melding.
 */
data class PeriodeManglendeInnbetalingDto(
    val antall: Long,
    val ikkeBetalt: Long,
    val delvisBetalt: Long,
    val utenDetaljer: Long,
    val ubetalt: BigDecimal,
)

data class FakturaserierDto(
    val antall: Long,
    val personer: Long,
    val perKilde: Map<String, Long>,
    val perStatus: Map<String, Long>,
    val perIntervall: Map<String, Long>,
)

data class MeldingOmManglendeInnbetalingDto(
    val fakturanummer: String?,
    val fakturaserieReferanse: String,
    val datoFraOebs: LocalDate?,
    val mottatt: Instant,
)

data class KjoeringDto(
    val dato: LocalDate,
    val sendt: Long,
    val beloep: BigDecimal,
    val feiletNoenGang: Long,
    val feilNaa: Long,
    val erstattetFoerKjoering: Long,
    val ikkeBestilt: Long,
    val manglendeInnbetaling: ManglendeInnbetalingDto,
)

data class ManglendeInnbetalingDto(
    val antall: Long,
    val sumUbetalt: BigDecimal,
    val akkumulert: AkkumulertDto,
)

data class AkkumulertDto(
    val d30: Long,
    val d60: Long,
    val d90: Long,
    val d120: Long,
    val d180: Long,
    val d365: Long,
)

data class NesteKjoeringDto(
    val dato: LocalDate,
    val antall: Long,
    val beloep: BigDecimal,
)

data class NaatilstandDto(
    val forfaltIkkeBestilt: ForfaltIkkeBestiltDto,
    val feil: FeilDto,
    val mottak: MottakDto,
    val sendtFoerBestillingsdato: SendtFoerBestillingsdatoDto,
    val muligDobbeltfakturering: List<MuligDobbeltfaktureringDto>,
    val koblingsfakturaer: Long,
)

data class ForfaltIkkeBestiltDto(
    val antall: Long,
    val eldsteBestillingsdato: LocalDate?,
    val eksempler: List<FakturaEksempelDto>,
)

data class SendtFoerBestillingsdatoDto(
    val antall: Long,
    val eksempler: List<FakturaEksempelDto>,
)

data class FakturaEksempelDto(
    val fakturaReferanse: String,
    val fakturaserieReferanse: String,
    val datoBestilt: LocalDate,
)

data class FeilDto(
    val uloest: Long,
    val eldsteFeilDato: LocalDate?,
)

data class MottakDto(
    val lytterKjoerer: Boolean?,
    val antallMottaksfeil: Long,
    val sisteMottaksfeil: Instant?,
    val sisteStatusmelding: Instant?,
    val manglendeInnbetalingIkkeVideresendt: Long,
)

data class MuligDobbeltfaktureringDto(
    val fakturaserieReferanser: List<String>,
)
