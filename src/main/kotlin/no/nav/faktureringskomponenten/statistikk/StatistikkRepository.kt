package no.nav.faktureringskomponenten.statistikk

import no.nav.faktureringskomponenten.service.FakturaGenerator
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Bare lesende SQL-aggregater for øyeblikksbildet. Ingen entiteter lastes, og fødselsnummer brukes bare til
 * gruppering i databasen – det returneres aldri.
 */
@Repository
class StatistikkRepository(private val jdbc: NamedParameterJdbcTemplate) {

    fun settTidsgrense() {
        jdbc.jdbcTemplate.execute("SET LOCAL statement_timeout = '30s'")
    }

    fun kjoeringer(idag: LocalDate): List<KjoeringDto> = jdbc.query(
        """
        WITH $FAKTURA, $FEILET_OG_MANGLENDE
        SELECT ft.dato_bestilt AS dato,
               COUNT(*) FILTER (WHERE ft.sendt) AS sendt,
               COALESCE(SUM(ft.beloep) FILTER (WHERE ft.sendt), 0) AS beloep,
               COUNT(*) FILTER (WHERE ft.status = 'FEIL' OR fe.faktura_id IS NOT NULL) AS feilet_noen_gang,
               COUNT(*) FILTER (WHERE ft.status = 'FEIL') AS feil_naa,
               COUNT(*) FILTER (WHERE ft.status = 'AVBRUTT') AS erstattet,
               (SELECT COUNT(*) FROM faktura o WHERE o.status = 'OPPRETTET' AND o.dato_bestilt = ft.dato_bestilt) AS ikke_bestilt,
               COUNT(m.faktura_id) AS manglende,
               COALESCE(SUM(m.ubetalt), 0) AS sum_ubetalt,
               COUNT(*) FILTER (WHERE m.foerste_dato <= ft.dato_bestilt + 30) AS d30,
               COUNT(*) FILTER (WHERE m.foerste_dato <= ft.dato_bestilt + 60) AS d60,
               COUNT(*) FILTER (WHERE m.foerste_dato <= ft.dato_bestilt + 90) AS d90,
               COUNT(*) FILTER (WHERE m.foerste_dato <= ft.dato_bestilt + 120) AS d120,
               COUNT(*) FILTER (WHERE m.foerste_dato <= ft.dato_bestilt + 180) AS d180,
               COUNT(*) FILTER (WHERE m.foerste_dato <= ft.dato_bestilt + 365) AS d365
        FROM faktura_total ft
        LEFT JOIN feilet fe ON fe.faktura_id = ft.id
        LEFT JOIN manglende m ON m.faktura_id = ft.id
        WHERE ft.kvartalskjoering AND ft.dato_bestilt <= :idag
        GROUP BY ft.dato_bestilt
        ORDER BY ft.dato_bestilt
        """,
        parametre("idag" to idag, "simulert" to SIMULERT_MANGLENDE_INNBETALING)
    ) { rs, _ ->
        KjoeringDto(
            dato = rs.getObject("dato", LocalDate::class.java),
            sendt = rs.getLong("sendt"),
            beloep = rs.getBigDecimal("beloep"),
            feiletNoenGang = rs.getLong("feilet_noen_gang"),
            feilNaa = rs.getLong("feil_naa"),
            erstattetFoerKjoering = rs.getLong("erstattet"),
            ikkeBestilt = rs.getLong("ikke_bestilt"),
            manglendeInnbetaling = ManglendeInnbetalingDto(
                antall = rs.getLong("manglende"),
                sumUbetalt = rs.getBigDecimal("sum_ubetalt"),
                akkumulert = AkkumulertDto(
                    d30 = rs.getLong("d30"),
                    d60 = rs.getLong("d60"),
                    d90 = rs.getLong("d90"),
                    d120 = rs.getLong("d120"),
                    d180 = rs.getLong("d180"),
                    d365 = rs.getLong("d365"),
                ),
            ),
        )
    }

    /** Planlagte fakturaer (alle typer) med bestillingsdato [dato]. */
    fun planlagt(dato: LocalDate): NesteKjoeringDto = jdbc.queryForObject(
        """
        WITH $FAKTURA
        SELECT COUNT(*) AS antall, COALESCE(SUM(beloep), 0) AS beloep
        FROM faktura_total
        WHERE status = 'OPPRETTET' AND dato_bestilt = :dato
        """,
        parametre("dato" to dato)
    ) { rs, _ -> NesteKjoeringDto(dato, rs.getLong("antall"), rs.getBigDecimal("beloep")) }

    fun forfaltIkkeBestilt(idag: LocalDate): ForfaltIkkeBestiltDto {
        val (antall, eksempler) = fakturaEksempler("ft.status = 'OPPRETTET' AND ft.dato_bestilt < :idag", idag)
        return ForfaltIkkeBestiltDto(antall, eksempler.firstOrNull()?.datoBestilt, eksempler)
    }

    fun sendtFoerBestillingsdato(idag: LocalDate): SendtFoerBestillingsdatoDto {
        val (antall, eksempler) = fakturaEksempler("ft.sendt AND ft.dato_bestilt > :idag", idag)
        return SendtFoerBestillingsdatoDto(antall, eksempler)
    }

    /** Antall og inntil 20 eksempler, eldste først. */
    private fun fakturaEksempler(betingelse: String, idag: LocalDate): Pair<Long, List<FakturaEksempelDto>> {
        var antall = 0L
        val eksempler = jdbc.query(
            """
            WITH $FAKTURA
            SELECT ft.referanse_nr, ft.serie_referanse, ft.dato_bestilt, COUNT(*) OVER () AS antall
            FROM faktura_total ft
            WHERE $betingelse
            ORDER BY ft.dato_bestilt, ft.id
            LIMIT 20
            """,
            parametre("idag" to idag)
        ) { rs, _ ->
            antall = rs.getLong("antall")
            FakturaEksempelDto(
                fakturaReferanse = rs.getString("referanse_nr"),
                fakturaserieReferanse = rs.getString("serie_referanse"),
                datoBestilt = rs.getObject("dato_bestilt", LocalDate::class.java),
            )
        }
        return antall to eksempler
    }

    /**
     * Fakturaer som står i FEIL nå, og den tidligste FEIL-meldingen fra OEBS på dem. Feilmeldingen er fritekst
     * og kan inneholde personopplysninger, så den tas ikke med.
     */
    fun feil(): FeilDto = jdbc.queryForObject(
        """
        SELECT (SELECT COUNT(*) FROM faktura WHERE status = 'FEIL') AS uloest,
               (SELECT MIN(e.dato) FROM ekstern_faktura_status e
                  JOIN faktura f ON f.id = e.faktura_id
                 WHERE f.status = 'FEIL' AND e.status = 'FEIL') AS eldste
        """,
        parametre()
    ) { rs, _ -> FeilDto(rs.getLong("uloest"), rs.getObject("eldste", LocalDate::class.java)) }

    fun mottak(lytterKjoerer: Boolean?): MottakDto = jdbc.queryForObject(
        """
        SELECT (SELECT COUNT(*) FROM faktura_mottak_feil) AS antall_feil,
               (SELECT MAX(opprettet_tidspunkt) FROM faktura_mottak_feil) AS siste_feil,
               (SELECT MAX(opprettet_tidspunkt) FROM ekstern_faktura_status) AS siste_statusmelding,
               (SELECT COUNT(*) FROM ekstern_faktura_status
                 WHERE status = 'MANGLENDE_INNBETALING' AND sendt IS NOT TRUE
                   AND feilmelding IS DISTINCT FROM :simulert) AS ikke_videresendt
        """,
        parametre("simulert" to SIMULERT_MANGLENDE_INNBETALING)
    ) { rs, _ ->
        MottakDto(
            lytterKjoerer = lytterKjoerer,
            antallMottaksfeil = rs.getLong("antall_feil"),
            sisteMottaksfeil = rs.getUtc("siste_feil"),
            sisteStatusmelding = rs.getUtc("siste_statusmelding"),
            manglendeInnbetalingIkkeVideresendt = rs.getLong("ikke_videresendt"),
        )
    }

    /**
     * Personer med planlagte trygdeavgiftsfakturaer i flere serier med overlappende periode. Grupperes på
     * fødselsnummer, men bare seriereferansene returneres.
     */
    fun muligDobbeltfakturering(): List<MuligDobbeltfaktureringDto> = jdbc.query(
        """
        WITH aktive AS (
            SELECT DISTINCT fs.id, fs.referanse, fs.fodselsnummer, fs.startdato, fs.sluttdato
            FROM fakturaserie fs
            JOIN faktura f ON f.fakturaserie_id = fs.id
            WHERE f.status = 'OPPRETTET' AND fs.faktura_gjelder_innbetalingstype = 'TRYGDEAVGIFT'
        ),
        overlapp AS (
            SELECT a.fodselsnummer, a.referanse AS ra, b.referanse AS rb
            FROM aktive a
            JOIN aktive b ON a.fodselsnummer = b.fodselsnummer AND a.id < b.id
             AND a.startdato <= b.sluttdato AND b.startdato <= a.sluttdato
        )
        SELECT string_agg(DISTINCT x.referanse, ',' ORDER BY x.referanse) AS referanser
        FROM (SELECT fodselsnummer, ra AS referanse FROM overlapp
              UNION SELECT fodselsnummer, rb FROM overlapp) x
        GROUP BY x.fodselsnummer
        ORDER BY 1
        """,
        parametre()
    ) { rs, _ -> MuligDobbeltfaktureringDto(rs.getString("referanser").split(",")) }

    /**
     * Meldinger om manglende innbetaling som er videresendt til Melosys, til avstemming mot prosessene der.
     * Fakturanummeret overskrives av hver statusmelding og blir tomt når meldingen mangler det, så det kan være null.
     */
    fun meldingerOmManglendeInnbetaling(): List<MeldingOmManglendeInnbetalingDto> = jdbc.query(
        """
        SELECT NULLIF(f.eksternt_fakturanummer, '') AS eksternt_fakturanummer, fs.referanse, e.dato, e.opprettet_tidspunkt
        FROM ekstern_faktura_status e
        JOIN faktura f ON f.id = e.faktura_id
        JOIN fakturaserie fs ON fs.id = f.fakturaserie_id
        WHERE e.status = 'MANGLENDE_INNBETALING' AND e.sendt
          AND e.feilmelding IS DISTINCT FROM :simulert
        ORDER BY e.opprettet_tidspunkt, e.id
        """,
        parametre("simulert" to SIMULERT_MANGLENDE_INNBETALING)
    ) { rs, _ ->
        MeldingOmManglendeInnbetalingDto(
            fakturanummer = rs.getString("eksternt_fakturanummer"),
            fakturaserieReferanse = rs.getString("referanse"),
            datoFraOebs = rs.getObject("dato", LocalDate::class.java),
            mottatt = rs.getUtc("opprettet_tidspunkt")!!,
        )
    }

    fun antallKoblingsfakturaer(): Long = jdbc.queryForObject(
        "WITH $FAKTURA SELECT COUNT(*) FROM faktura_total WHERE kobling",
        parametre(),
        Long::class.java
    )!!

    /** Planlagte fakturaer (alle typer) fordelt rundt neste kvartalskjøring. */
    fun planlagtFramover(idag: LocalDate, nesteKjoering: LocalDate): PlanlagtDto = jdbc.queryForObject(
        """
        WITH $FAKTURA
        SELECT ${antallOgBeloep("forfalt", "dato_bestilt < :idag")},
               ${antallOgBeloep("foer", "dato_bestilt >= :idag AND dato_bestilt < :neste")},
               ${antallOgBeloep("neste", "dato_bestilt = :neste")},
               ${antallOgBeloep("senere", "dato_bestilt > :neste")},
               ${antallOgBeloep("totalt", "true")},
               MAX(dato_bestilt) AS siste,
               COUNT(DISTINCT fakturaserie_id) AS serier,
               COUNT(DISTINCT fodselsnummer) AS personer
        FROM faktura_total
        WHERE status = 'OPPRETTET'
        """,
        parametre("idag" to idag, "neste" to nesteKjoering)
    ) { rs, _ ->
        PlanlagtDto(
            forfalt = rs.antallOgBeloep("forfalt"),
            foerNesteKjoering = rs.antallOgBeloep("foer"),
            iNesteKjoering = rs.antallOgBeloep("neste"),
            senere = rs.antallOgBeloep("senere"),
            totalt = rs.antallOgBeloep("totalt"),
            sisteBestillingsdato = rs.getObject("siste", LocalDate::class.java),
            serier = rs.getLong("serier"),
            personer = rs.getLong("personer"),
        )
    }

    /** Planlagte fakturaer per bestillingskvartal, fra inneværende kvartal og framover. */
    fun planlagtPerKvartal(idag: LocalDate): List<PlanlagtKvartalDto> = jdbc.query(
        """
        WITH $FAKTURA
        SELECT to_char(dato_bestilt, 'YYYY-"K"Q') AS kvartal, COUNT(*) AS antall, SUM(beloep) AS beloep
        FROM faktura_total
        WHERE status = 'OPPRETTET' AND dato_bestilt >= date_trunc('quarter', CAST(:idag AS date))
        GROUP BY 1
        ORDER BY 1
        """,
        parametre("idag" to idag)
    ) { rs, _ -> PlanlagtKvartalDto(rs.getString("kvartal"), rs.getLong("antall"), rs.getBigDecimal("beloep")) }

    /** Sendte fakturaer utenom kvartalskjøringene, per bestillingskvartal, eldste først. */
    fun utenomPerKvartal(idag: LocalDate): List<UtenomKvartalDto> = jdbc.query(
        """
        WITH $SENDTE
        SELECT to_char(dato_bestilt, 'YYYY-"K"Q') AS kvartal,
               COUNT(*) AS sendt,
               SUM(faktura_sum) AS beloep,
               ${TYPER.joinToString { antallOgBeloep("type_$it", "type = '$it'", "faktura_sum") }},
               ${antallPer("kilde", KILDER)},
               COUNT(*) FILTER (WHERE feilet) AS feilet,
               COUNT(*) FILTER (WHERE manglende) AS manglende
        FROM sendte
        WHERE maate = 'UTENOM'
        GROUP BY 1
        ORDER BY 1
        """,
        parametre("idag" to idag, "simulert" to SIMULERT_MANGLENDE_INNBETALING)
    ) { rs, _ ->
        UtenomKvartalDto(
            kvartal = rs.getString("kvartal"),
            sendt = rs.getLong("sendt"),
            beloep = rs.getBigDecimal("beloep"),
            perType = TYPER.associateWith { rs.antallOgBeloep("type_$it") },
            perKilde = rs.antallPer("kilde", KILDER),
            feiletNoenGang = rs.getLong("feilet"),
            manglendeInnbetaling = rs.getLong("manglende"),
        )
    }

    /**
     * Sendte fakturaer per kvartal og år (bestillingsdato), per år perioden gjelder og alt samlet. GJELDER_AAR
     * regnes per fakturalinje: en faktura med linjer i to år telles i begge, med linjesummen for hvert år.
     */
    fun perioder(idag: LocalDate): List<PeriodeDto> = jdbc.query(
        """
        WITH $SENDTE,
        perioder AS (
            SELECT id, 'KVARTAL' AS periodetype, to_char(dato_bestilt, 'YYYY-"K"Q') AS noekkel, faktura_sum AS beloep FROM sendte
            UNION ALL
            SELECT id, 'AAR', to_char(dato_bestilt, 'YYYY'), faktura_sum FROM sendte
            UNION ALL
            SELECT faktura_id, 'GJELDER_AAR', to_char(periode_fra, 'YYYY'), SUM(belop)
            FROM faktura_linje GROUP BY faktura_id, to_char(periode_fra, 'YYYY')
            UNION ALL
            SELECT id, 'ALT', 'alt', faktura_sum FROM sendte
        )
        SELECT p.periodetype, p.noekkel,
               COUNT(*) AS sendt,
               COALESCE(SUM(beloep) FILTER (WHERE beloep > 0), 0) AS fakturert,
               COALESCE(SUM(beloep) FILTER (WHERE beloep < 0), 0) AS kreditert,
               SUM(beloep) AS netto,
               COUNT(DISTINCT s.fodselsnummer) AS personer,
               ${TYPER.joinToString { antallOgBeloep("type_$it", "type = '$it'") }},
               ${BESTILLINGSMAATER.joinToString { antallOgBeloep("maate_$it", "maate = '$it'") }},
               ${antallPer("status", SENDT_STATUSER)},
               COUNT(*) FILTER (WHERE feilet) AS feil_noen_gang,
               COUNT(*) FILTER (WHERE status = 'FEIL') AS feil_uloest,
               COUNT(*) FILTER (WHERE manglende) AS manglende,
               COUNT(*) FILTER (WHERE ubetalt = fakturert_oebs) AS ikke_betalt,
               COUNT(*) FILTER (WHERE ubetalt <> fakturert_oebs) AS delvis_betalt,
               COUNT(*) FILTER (WHERE manglende AND (ubetalt IS NULL OR fakturert_oebs IS NULL)) AS uten_detaljer,
               COALESCE(SUM(ubetalt), 0) AS ubetalt
        FROM perioder p
        JOIN sendte s ON s.id = p.id
        GROUP BY p.periodetype, p.noekkel
        ORDER BY array_position(ARRAY['KVARTAL', 'AAR', 'GJELDER_AAR', 'ALT'], p.periodetype), p.noekkel DESC
        """,
        parametre("idag" to idag, "simulert" to SIMULERT_MANGLENDE_INNBETALING)
    ) { rs, _ ->
        val feilNoenGang = rs.getLong("feil_noen_gang")
        val feilUloest = rs.getLong("feil_uloest")
        PeriodeDto(
            type = rs.getString("periodetype"),
            noekkel = rs.getString("noekkel"),
            sendt = rs.getLong("sendt"),
            fakturert = rs.getBigDecimal("fakturert"),
            kreditert = rs.getBigDecimal("kreditert"),
            netto = rs.getBigDecimal("netto"),
            personer = rs.getLong("personer"),
            perType = TYPER.associateWith { rs.antallOgBeloep("type_$it") },
            perBestillingsmaate = BESTILLINGSMAATER.associateWith { rs.antallOgBeloep("maate_$it") },
            perStatus = rs.antallPer("status", SENDT_STATUSER),
            feil = PeriodeFeilDto(noenGang = feilNoenGang, loest = feilNoenGang - feilUloest, uloest = feilUloest),
            manglendeInnbetaling = PeriodeManglendeInnbetalingDto(
                antall = rs.getLong("manglende"),
                ikkeBetalt = rs.getLong("ikke_betalt"),
                delvisBetalt = rs.getLong("delvis_betalt"),
                utenDetaljer = rs.getLong("uten_detaljer"),
                ubetalt = rs.getBigDecimal("ubetalt"),
            ),
        )
    }

    /** Alle fakturaserier, uansett status. */
    fun fakturaserier(): FakturaserierDto = jdbc.queryForObject(
        """
        SELECT COUNT(*) AS antall,
               COUNT(DISTINCT fodselsnummer) AS personer,
               ${antallPer("kilde", KILDER)},
               ${antallPer("status", SERIESTATUSER)},
               ${antallPer("intervall", INTERVALLER)}
        FROM (SELECT fs.fodselsnummer, fs.status::text AS status, fs.intervall::text AS intervall, $KILDE AS kilde
              FROM fakturaserie fs) fs
        """,
        parametre()
    ) { rs, _ ->
        FakturaserierDto(
            antall = rs.getLong("antall"),
            personer = rs.getLong("personer"),
            perKilde = rs.antallPer("kilde", KILDER),
            perStatus = rs.antallPer("status", SERIESTATUSER),
            perIntervall = rs.antallPer("intervall", INTERVALLER),
        )
    }

    /** To kolonner, `<navn>_antall` og `<navn>_beloep`, for radene som oppfyller [betingelse]. */
    private fun antallOgBeloep(navn: String, betingelse: String, beloep: String = "beloep") =
        "COUNT(*) FILTER (WHERE $betingelse) AS ${navn}_antall, " +
            "COALESCE(SUM($beloep) FILTER (WHERE $betingelse), 0) AS ${navn}_beloep"

    private fun ResultSet.antallOgBeloep(navn: String) =
        AntallBeloepDto(getLong("${navn}_antall"), getBigDecimal("${navn}_beloep"))

    /** Én kolonne `<kolonne>_<verdi>` per verdi med antall rader der [kolonne] har verdien. */
    private fun antallPer(kolonne: String, verdier: List<String>) =
        verdier.joinToString { "COUNT(*) FILTER (WHERE $kolonne = '$it') AS ${kolonne}_$it" }

    private fun ResultSet.antallPer(kolonne: String, verdier: List<String>) =
        verdier.associateWith { getLong("${kolonne}_$it") }

    /** Alle spørringer får bestillingsdagen, siden [FAKTURA] bruker den. */
    private fun parametre(vararg parametre: Pair<String, Any>): Map<String, Any> =
        mapOf("bestillingsdag" to BESTILLINGSDAG, *parametre)

    // Hibernate lagrer tidspunkter som UTC-veggtid i timestamp-kolonnene
    private fun ResultSet.getUtc(kolonne: String): Instant? =
        getObject(kolonne, LocalDateTime::class.java)?.toInstant(ZoneOffset.UTC)

    companion object {
        const val SIMULERT_MANGLENDE_INNBETALING = "Simulert manglende innbetaling"

        private val TYPER = listOf("VANLIG", "AVREGNING_TILLEGG", "AVREGNING_FRADRAG", "AARSAVREGNING", "KREDITERING")
        private val KILDER = listOf("VEDTAK", "EOS_PENSJONIST", "SATSENDRING", "AARSAVREGNING", "ANNET")
        private val BESTILLINGSMAATER = listOf("KVARTALSKJOERING", "UTENOM")
        private val SENDT_STATUSER = listOf("BESTILT", "MANGLENDE_INNBETALING", "FEIL", "INNE_I_OEBS")
        private val SERIESTATUSER = listOf("OPPRETTET", "UNDER_BESTILLING", "ERSTATTET", "KANSELLERT", "FERDIG")
        private val INTERVALLER = listOf("KVARTAL", "SINGEL", "MANEDLIG")

        /**
         * Hvilken flyt i Melosys som opprettet serien, utledet fra referanse_bruker (teksten på fakturaen til
         * bruker). Tekstene settes i melosys-api; ukjente tekster havner i ANNET.
         */
        private const val KILDE = """
            CASE
                WHEN fs.referanse_bruker LIKE 'Vedtak om medlemskap%' THEN 'VEDTAK'
                WHEN fs.referanse_bruker LIKE 'Informasjon om trygdeavgift%' THEN 'EOS_PENSJONIST'
                WHEN fs.referanse_bruker LIKE 'Faktura for årlig satsoppdatering%' THEN 'SATSENDRING'
                WHEN fs.referanse_bruker LIKE 'Årsavregning%' THEN 'AARSAVREGNING'
                ELSE 'ANNET'
            END"""

        /**
         * Dagen i måneden kvartalskjøringen bestiller, hentet fra FakturaGenerator så den bare bestemmes ett sted.
         * `const val` inlines ved kompilering, så statistikk får ingen avhengighet til service (se ArkitekturTest).
         *
         * TODO: Endres bestillingsdagen, gjenkjennes ikke lenger historiske kvartalskjøringer med gammel dag – de
         *  blir «utenom» kvartalskjøringene. Historikken må da løses, f.eks. med en regel per periode.
         */
        const val BESTILLINGSDAG = FakturaGenerator.BESTILLINGSDAG

        /**
         * Alle fakturaer med beløp (sum av linjene) og disse utledningene:
         * - kobling: avregningsfaktura med beløp 0. Får status BESTILT bare for å koble en tidligere faktura
         *   til avregningen, og sendes aldri til OEBS.
         * - type: AARSAVREGNING etter seriens innbetalingstype; KREDITERING når beløpet er negativt og fakturaen
         *   ikke er avregning (eller er kreditnota); ellers AVREGNING_FRADRAG/_TILLEGG etter fortegnet, eller VANLIG.
         * - kvartalskjoering: vanlig kvartalsfaktura for trygdeavgift (ikke avregning, ikke kreditering) med
         *   bestillingsdato på [BESTILLINGSDAG] i måneden før kvartalet til første linje, som i
         *   FakturaGenerator.utledBestillingsdato.
         * - sendt: har gått til OEBS, altså bestilt eller senere status og ikke kobling.
         *
         * faktura_total inneholder fødselsnummer, som bare skal brukes til COUNT(DISTINCT …). Velg aldri `*`
         * eller `ft.*` ut av den (eller CTE-er som bygger på den), så havner det ikke i svaret.
         */
        private const val FAKTURA = """
        faktura_beloep AS (
            SELECT f.id,
                   f.referanse_nr,
                   f.fakturaserie_id,
                   fs.referanse AS serie_referanse,
                   fs.fodselsnummer,
                   $KILDE AS kilde,
                   f.status::text AS status,
                   f.dato_bestilt,
                   COALESCE(l.sum, 0) AS beloep,
                   f.avregning_faktura_id IS NOT NULL AND COALESCE(l.sum, 0) = 0 AS kobling,
                   CASE WHEN f.avregning_faktura_id IS NOT NULL AND COALESCE(l.sum, 0) = 0 THEN 'KOBLING'
                        WHEN fs.faktura_gjelder_innbetalingstype::text = 'AARSAVREGNING' THEN 'AARSAVREGNING'
                        WHEN COALESCE(l.sum, 0) < 0 AND (f.avregning_faktura_id IS NULL OR f.er_kreditnota) THEN 'KREDITERING'
                        WHEN f.avregning_faktura_id IS NOT NULL AND l.sum < 0 THEN 'AVREGNING_FRADRAG'
                        WHEN f.avregning_faktura_id IS NOT NULL THEN 'AVREGNING_TILLEGG'
                        ELSE 'VANLIG' END AS type,
                   COALESCE(f.avregning_faktura_id IS NULL AND l.sum > 0
                       AND fs.faktura_gjelder_innbetalingstype::text = 'TRYGDEAVGIFT'
                       AND fs.intervall::text = 'KVARTAL'
                       AND f.dato_bestilt = (date_trunc('quarter', l.forste_fra::timestamp) - interval '1 month')::date + (:bestillingsdag - 1),
                       false) AS kvartalskjoering
            FROM faktura f
            JOIN fakturaserie fs ON fs.id = f.fakturaserie_id
            LEFT JOIN (SELECT faktura_id, SUM(belop) AS sum, MIN(periode_fra) AS forste_fra
                       FROM faktura_linje GROUP BY faktura_id) l ON l.faktura_id = f.id
        ),
        faktura_total AS (
            SELECT *, status IN ('BESTILT', 'FEIL', 'MANGLENDE_INNBETALING', 'INNE_I_OEBS') AND NOT kobling AS sendt
            FROM faktura_beloep
        )
        """

        /**
         * Fakturaer som har fått FEIL fra OEBS, og fakturaer med (ikke-simulert) melding om manglende innbetaling:
         * første dato, og ubetalt beløp og fakturabeløp fra siste melding. Krever parameteren :simulert.
         */
        private const val FEILET_OG_MANGLENDE = """
        feilet AS (
            SELECT DISTINCT faktura_id FROM ekstern_faktura_status WHERE status = 'FEIL'
        ),
        manglende AS (
            SELECT DISTINCT ON (faktura_id) faktura_id,
                   MIN(dato) OVER (PARTITION BY faktura_id) AS foerste_dato,
                   ubetalt_belop AS ubetalt,
                   faktura_belop AS fakturert_oebs
            FROM ekstern_faktura_status
            WHERE status = 'MANGLENDE_INNBETALING' AND feilmelding IS DISTINCT FROM :simulert
            ORDER BY faktura_id, dato DESC, id DESC
        )
        """

        /**
         * Sendte fakturaer med bestillingsmåte, feil og manglende innbetaling. En faktura sendt før
         * bestillingsdatoen er ikke en del av noen kvartalskjøring ennå og regnes som UTENOM. En faktura som står
         * i MANGLENDE_INNBETALING bare etter simulerte meldinger regnes som BESTILT.
         * Krever parameterne :idag og :simulert.
         */
        private const val SENDTE = """
        $FAKTURA, $FEILET_OG_MANGLENDE,
        sendte AS (
            SELECT ft.id, ft.dato_bestilt, ft.beloep AS faktura_sum, ft.type, ft.kilde, ft.fodselsnummer,
                   CASE WHEN ft.status = 'MANGLENDE_INNBETALING' AND m.faktura_id IS NULL THEN 'BESTILT'
                        ELSE ft.status END AS status,
                   CASE WHEN ft.kvartalskjoering AND ft.dato_bestilt <= :idag THEN 'KVARTALSKJOERING' ELSE 'UTENOM' END AS maate,
                   ft.status = 'FEIL' OR fe.faktura_id IS NOT NULL AS feilet,
                   m.faktura_id IS NOT NULL AS manglende,
                   m.ubetalt,
                   m.fakturert_oebs
            FROM faktura_total ft
            LEFT JOIN feilet fe ON fe.faktura_id = ft.id
            LEFT JOIN manglende m ON m.faktura_id = ft.id
            WHERE ft.sendt
        )
        """
    }
}
