package no.nav.faktureringskomponenten.statistikk

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
        WITH $FAKTURA,
        feilet AS (
            SELECT DISTINCT faktura_id FROM ekstern_faktura_status WHERE status = 'FEIL'
        ),
        manglende AS (
            SELECT faktura_id,
                   MIN(dato) AS foerste_dato,
                   (ARRAY_AGG(ubetalt_belop ORDER BY dato DESC, id DESC))[1] AS ubetalt
            FROM ekstern_faktura_status
            WHERE status = 'MANGLENDE_INNBETALING' AND feilmelding IS DISTINCT FROM :simulert
            GROUP BY faktura_id
        )
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
        mapOf("idag" to idag, "simulert" to SIMULERT_MANGLENDE_INNBETALING)
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
        mapOf("dato" to dato)
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
            mapOf("idag" to idag)
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
     * Fakturaer i FEIL gruppert på siste feilmelding fra OEBS. Meldingen er fritekst og kan inneholde
     * fødselsnummer eller organisasjonsnummer, så tallrekker maskeres før grupperingen.
     */
    fun feilPerMelding(): List<FeilmeldingDto> = jdbc.query(
        """
        WITH siste_feil AS (
            SELECT DISTINCT ON (faktura_id) faktura_id, feilmelding, dato
            FROM ekstern_faktura_status
            WHERE status = 'FEIL'
            ORDER BY faktura_id, dato DESC, id DESC
        )
        SELECT LEFT(regexp_replace(COALESCE(sf.feilmelding, '(ingen feilmelding)'), :tallmoenster, '***', 'g'), 300) AS melding,
               COUNT(*) AS antall,
               MIN(sf.dato) AS eldste
        FROM faktura f
        LEFT JOIN siste_feil sf ON sf.faktura_id = f.id
        WHERE f.status = 'FEIL'
        GROUP BY 1
        ORDER BY 2 DESC, 1
        """,
        mapOf("tallmoenster" to TALLMOENSTER)
    ) { rs, _ ->
        FeilmeldingDto(rs.getString("melding"), rs.getLong("antall"), rs.getObject("eldste", LocalDate::class.java))
    }

    fun mottak(lytterKjoerer: Boolean?): MottakDto = jdbc.queryForObject(
        """
        SELECT (SELECT COUNT(*) FROM faktura_mottak_feil) AS antall_feil,
               (SELECT MAX(opprettet_tidspunkt) FROM faktura_mottak_feil) AS siste_feil,
               (SELECT MAX(opprettet_tidspunkt) FROM ekstern_faktura_status) AS siste_statusmelding,
               (SELECT COUNT(*) FROM ekstern_faktura_status
                 WHERE status = 'MANGLENDE_INNBETALING' AND sendt IS NOT TRUE
                   AND feilmelding IS DISTINCT FROM :simulert) AS ikke_videresendt
        """,
        mapOf("simulert" to SIMULERT_MANGLENDE_INNBETALING)
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
        emptyMap<String, Any>()
    ) { rs, _ -> MuligDobbeltfaktureringDto(rs.getString("referanser").split(",")) }

    fun antallKoblingsfakturaer(): Long = jdbc.queryForObject(
        "WITH $FAKTURA SELECT COUNT(*) FROM faktura_total WHERE kobling",
        emptyMap<String, Any>(),
        Long::class.java
    )!!

    // Hibernate lagrer tidspunkter som UTC-veggtid i timestamp-kolonnene
    private fun ResultSet.getUtc(kolonne: String): Instant? =
        getObject(kolonne, LocalDateTime::class.java)?.toInstant(ZoneOffset.UTC)

    companion object {
        const val SIMULERT_MANGLENDE_INNBETALING = "Simulert manglende innbetaling"

        /**
         * Fem eller flere sifre, eventuelt skilt med ett tegn som ikke er bokstav eller siffer
         * (mellomrom, NBSP, punktum, bindestrek, skråstrek, understrek osv.). Sendes som parameter (`?:`).
         */
        const val TALLMOENSTER = """\d(?:[^[:alnum:]]?\d){4,}"""

        /**
         * Alle fakturaer med beløp (sum av linjene) og tre utledninger:
         * - kobling: avregningsfaktura med beløp 0. Får status BESTILT bare for å koble en tidligere faktura
         *   til avregningen, og sendes aldri til OEBS.
         * - kvartalskjoering: vanlig kvartalsfaktura for trygdeavgift (ikke avregning, ikke kreditering) med
         *   bestillingsdato den 19. i måneden før kvartalet til første linje, som i
         *   FakturaGenerator.utledBestillingsdato.
         * - sendt: har gått til OEBS, altså bestilt eller senere status og ikke kobling.
         */
        private const val FAKTURA = """
        faktura_beloep AS (
            SELECT f.id,
                   f.referanse_nr,
                   fs.referanse AS serie_referanse,
                   f.status::text AS status,
                   f.dato_bestilt,
                   COALESCE(l.sum, 0) AS beloep,
                   f.avregning_faktura_id IS NOT NULL AND COALESCE(l.sum, 0) = 0 AS kobling,
                   COALESCE(f.avregning_faktura_id IS NULL AND l.sum > 0
                       AND fs.faktura_gjelder_innbetalingstype::text = 'TRYGDEAVGIFT'
                       AND fs.intervall::text = 'KVARTAL'
                       AND f.dato_bestilt = (date_trunc('quarter', l.forste_fra::timestamp) - interval '1 month' + interval '18 days')::date,
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
    }
}
