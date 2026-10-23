package no.nav.faktureringskomponenten.statistikk

import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
class StatistikkService(
    private val statistikkRepository: StatistikkRepository,
    private val kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry,
) {

    /** Alle spørringer i samme skrivebeskyttede transaksjon, så tallene er fra ett og samme øyeblikk. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun hentOyeblikksbilde(naa: Instant = Instant.now()): StatistikkOyeblikksbildeDto {
        val start = Instant.now()
        statistikkRepository.settTidsgrense()
        val idag = LocalDate.ofInstant(naa, ZoneId.of("Europe/Oslo"))

        val kjoeringer = statistikkRepository.kjoeringer(idag)
        val planlagtIdag = statistikkRepository.planlagt(idag)
        // Er det kjøringsdag og noe står igjen, er kjøringen ikke ferdig ennå
        val nesteKjoering = if (erKjoeringsdato(idag) && planlagtIdag.antall > 0) planlagtIdag
        else statistikkRepository.planlagt(nesteKjoeringsdatoEtter(idag))

        val naatilstand = NaatilstandDto(
            forfaltIkkeBestilt = statistikkRepository.forfaltIkkeBestilt(idag),
            feil = statistikkRepository.feil(),
            mottak = statistikkRepository.mottak(lytterKjoerer()),
            sendtFoerBestillingsdato = statistikkRepository.sendtFoerBestillingsdato(idag),
            muligDobbeltfakturering = statistikkRepository.muligDobbeltfakturering(),
            koblingsfakturaer = statistikkRepository.antallKoblingsfakturaer(),
        )

        return StatistikkOyeblikksbildeDto(
            versjon = 1,
            beregnet = naa,
            varighetMs = Duration.between(start, Instant.now()).toMillis(),
            idag = idag,
            kjoeringer = kjoeringer,
            nesteKjoering = nesteKjoering,
            naa = naatilstand,
        )
    }

    private fun lytterKjoerer(): Boolean? =
        runCatching { kafkaListenerEndpointRegistry.getListenerContainer("fakturaMottatt")?.isRunning }.getOrNull()

    private fun erKjoeringsdato(dato: LocalDate) = dato.dayOfMonth == 19 && dato.monthValue % 3 == 0

    /** Første 19.03, 19.06, 19.09 eller 19.12 etter [dato]. */
    private fun nesteKjoeringsdatoEtter(dato: LocalDate): LocalDate {
        var neste = dato.withDayOfMonth(19)
        if (neste <= dato) neste = neste.plusMonths(1)
        while (neste.monthValue % 3 != 0) neste = neste.plusMonths(1)
        return neste
    }
}
