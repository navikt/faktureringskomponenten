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
