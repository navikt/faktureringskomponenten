package no.nav.faktureringskomponenten.domain.repositories

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Aggregerte spørringer for fakturastatistikken i melosys-console. Alt summeres i databasen, så
 * ingen entiteter (og ingen fødselsnumre) lastes inn i minnet.
 *
 * Fakturatype utledes av [FAKTURA_TOTAL], se kommentaren der.
 */
@Repository
class FakturastatistikkRepository(private val jdbc: NamedParameterJdbcTemplate) {

    fun bestiltGruppert(fra: LocalDate, til: LocalDate): List<BestiltGruppe> = jdbc.query(
        """
        WITH $FAKTURA_TOTAL,
        siste_manglende AS (
            SELECT DISTINCT ON (e.faktura_id) e.faktura_id, e.faktura_belop, e.ubetalt_belop
            FROM ekstern_faktura_status e
            WHERE e.status = 'MANGLENDE_INNBETALING'
            ORDER BY e.faktura_id, e.id DESC
        )
        SELECT ft.status,
               ft.type,
               fs.faktura_gjelder_innbetalingstype::text AS innbetalingstype,
               COUNT(*) AS antall,
               COALESCE(SUM(ft.total) FILTER (WHERE ft.total > 0), 0) AS sum_debet,
               COALESCE(SUM(ft.total) FILTER (WHERE ft.total < 0), 0) AS sum_kredit,
               COUNT(*) FILTER (WHERE m.faktura_id IS NOT NULL AND m.ubetalt_belop = m.faktura_belop) AS ikke_betalt,
               COUNT(*) FILTER (WHERE m.faktura_id IS NOT NULL AND m.ubetalt_belop IS DISTINCT FROM m.faktura_belop) AS delvis_betalt,
               COALESCE(SUM(m.ubetalt_belop), 0) AS sum_ubetalt
        FROM faktura_total ft
        JOIN fakturaserie fs ON fs.id = ft.fakturaserie_id
        LEFT JOIN siste_manglende m ON m.faktura_id = ft.id AND ft.status = 'MANGLENDE_INNBETALING'
        WHERE ft.sendt AND ft.dato_bestilt BETWEEN :fra AND :til
        GROUP BY ft.status, ft.type, fs.faktura_gjelder_innbetalingstype
        """,
        mapOf("fra" to fra, "til" to til)
    ) { rs, _ ->
        BestiltGruppe(
            status = rs.getString("status"),
            type = rs.getString("type"),
            innbetalingstype = rs.getString("innbetalingstype"),
            antall = rs.getLong("antall"),
            sumDebet = rs.getBigDecimal("sum_debet"),
            sumKredit = rs.getBigDecimal("sum_kredit"),
            ikkeBetalt = rs.getLong("ikke_betalt"),
            delvisBetalt = rs.getLong("delvis_betalt"),
            sumUbetalt = rs.getBigDecimal("sum_ubetalt"),
        )
    }

    fun antallUnikePersonerBestilt(fra: LocalDate, til: LocalDate): Long = jdbc.queryForObject(
        """
        WITH $FAKTURA_TOTAL
        SELECT COUNT(DISTINCT fs.fodselsnummer)
        FROM faktura_total ft
        JOIN fakturaserie fs ON fs.id = ft.fakturaserie_id
        WHERE ft.sendt AND ft.dato_bestilt BETWEEN :fra AND :til
        """,
        mapOf("fra" to fra, "til" to til),
        Long::class.java
    ) ?: 0

    /** Nåtilstand: planlagte, forfalte og ventende fakturaer, i ett snapshot. */
    fun naatilstand(idag: LocalDate, naa: Instant): Naatilstand = jdbc.queryForObject(
        """
        WITH $FAKTURA_TOTAL
        SELECT
          COUNT(*) FILTER (WHERE ft.status = 'OPPRETTET' AND ft.dato_bestilt < :idag) AS forfalt,
          MIN(ft.dato_bestilt) FILTER (WHERE ft.status = 'OPPRETTET' AND ft.dato_bestilt < :idag) AS eldste_forfalt,
          COUNT(*) FILTER (WHERE $VENTER) AS venter,
          COUNT(*) FILTER (WHERE $VENTER AND ft.sist_endret >= :grense3d) AS venter_under3,
          COUNT(*) FILTER (WHERE $VENTER AND ft.sist_endret < :grense3d AND ft.sist_endret >= :grense7d) AS venter_3til7,
          COUNT(*) FILTER (WHERE $VENTER AND ft.sist_endret < :grense7d AND ft.sist_endret >= :grense30d) AS venter_7til30,
          COUNT(*) FILTER (WHERE $VENTER AND ft.sist_endret < :grense30d) AS venter_over30,
          MIN(ft.sist_endret) FILTER (WHERE $VENTER) AS eldste_venter,
          COUNT(*) FILTER (WHERE ft.type = 'KOBLING') AS koblingsfakturaer,
          COUNT(*) FILTER (WHERE ft.status = 'OPPRETTET') AS planlagt,
          COALESCE(SUM(ft.total) FILTER (WHERE ft.status = 'OPPRETTET'), 0) AS planlagt_beloep,
          COUNT(*) FILTER (WHERE ft.status = 'OPPRETTET' AND ft.dato_bestilt >= :idag AND ft.dato_bestilt < :om30d) AS planlagt_neste30,
          COALESCE(SUM(ft.total) FILTER (WHERE ft.status = 'OPPRETTET' AND ft.dato_bestilt >= :idag AND ft.dato_bestilt < :om30d), 0) AS planlagt_neste30_beloep,
          COUNT(DISTINCT ft.fakturaserie_id) FILTER (WHERE ft.status = 'OPPRETTET') AS serier_med_planlagte,
          COUNT(DISTINCT fs.fodselsnummer) FILTER (WHERE ft.status = 'OPPRETTET') AS personer_med_planlagte,
          COUNT(*) FILTER (WHERE ft.status = 'FEIL') AS feil,
          COUNT(*) FILTER (WHERE ft.status = 'MANGLENDE_INNBETALING') AS manglende_innbetaling
        FROM faktura_total ft
        JOIN fakturaserie fs ON fs.id = ft.fakturaserie_id
        """,
        mapOf(
            "idag" to idag,
            "om30d" to idag.plusDays(30),
            "grense3d" to utc(naa.minus(Duration.ofDays(3))),
            "grense7d" to utc(naa.minus(Duration.ofDays(7))),
            "grense30d" to utc(naa.minus(Duration.ofDays(30))),
        )
    ) { rs, _ ->
        Naatilstand(
            forfalt = rs.getLong("forfalt"),
            eldsteForfalt = rs.getLocalDate("eldste_forfalt"),
            venter = rs.getLong("venter"),
            venterUnder3Dager = rs.getLong("venter_under3"),
            venter3Til7Dager = rs.getLong("venter_3til7"),
            venter7Til30Dager = rs.getLong("venter_7til30"),
            venterOver30Dager = rs.getLong("venter_over30"),
            eldsteVenter = rs.getInstant("eldste_venter"),
            koblingsfakturaer = rs.getLong("koblingsfakturaer"),
            planlagt = rs.getLong("planlagt"),
            planlagtBeloep = rs.getBigDecimal("planlagt_beloep"),
            planlagtNeste30Dager = rs.getLong("planlagt_neste30"),
            planlagtNeste30DagerBeloep = rs.getBigDecimal("planlagt_neste30_beloep"),
            serierMedPlanlagte = rs.getLong("serier_med_planlagte"),
            personerMedPlanlagte = rs.getLong("personer_med_planlagte"),
            feil = rs.getLong("feil"),
            manglendeInnbetaling = rs.getLong("manglende_innbetaling"),
        )
    }!!

    /** De eldste fakturaene som er sendt til OEBS før [grense] uten at vi har fått kvittering. */
    fun eldsteUtenKvittering(grense: Instant, maks: Int): List<FakturaOppfoelging> = jdbc.query(
        """
        WITH $FAKTURA_TOTAL
        SELECT ft.referanse_nr, fs.referanse AS fakturaserie_referanse, ft.dato_bestilt, ft.sist_endret
        FROM faktura_total ft
        JOIN fakturaserie fs ON fs.id = ft.fakturaserie_id
        WHERE $VENTER AND ft.sist_endret < :grense
        ORDER BY ft.sist_endret, ft.id
        LIMIT :maks
        """,
        mapOf("grense" to utc(grense), "maks" to maks),
        ::tilFakturaOppfoelging
    )

    /** Planlagte fakturaer med bestillingsdato før [idag] – skulle vært plukket opp av bestillingsjobben. */
    fun eldsteForfalte(idag: LocalDate, maks: Int): List<FakturaOppfoelging> = jdbc.query(
        """
        WITH $FAKTURA_TOTAL
        SELECT ft.referanse_nr, fs.referanse AS fakturaserie_referanse, ft.dato_bestilt, ft.sist_endret
        FROM faktura_total ft
        JOIN fakturaserie fs ON fs.id = ft.fakturaserie_id
        WHERE ft.status = 'OPPRETTET' AND ft.dato_bestilt < :idag
        ORDER BY ft.dato_bestilt, ft.id
        LIMIT :maks
        """,
        mapOf("idag" to idag, "maks" to maks),
        ::tilFakturaOppfoelging
    )

    /**
     * Fakturaer i FEIL gruppert på siste feilmelding fra OEBS, flest først. Feilmeldingen er fritekst og kan
     * inneholde fødselsnummer, organisasjonsnummer eller fakturanummer, så tallrekker maskeres før grupperingen.
     */
    fun feilPerMelding(maks: Int): List<FeilmeldingGruppe> = jdbc.query(
        """
        WITH siste_feil AS (
            SELECT DISTINCT ON (e.faktura_id) e.faktura_id, e.feilmelding, e.dato
            FROM ekstern_faktura_status e
            WHERE e.status = 'FEIL'
            ORDER BY e.faktura_id, e.id DESC
        )
        SELECT LEFT(
                   regexp_replace(COALESCE(NULLIF(TRIM(sf.feilmelding), ''), :ingenMelding), :tallMoenster, :maske, 'g'),
                   :maksLengde
               ) AS melding,
               COUNT(*) AS antall,
               MIN(sf.dato) AS eldste
        FROM faktura f
        LEFT JOIN siste_feil sf ON sf.faktura_id = f.id
        WHERE f.status = 'FEIL'
        GROUP BY 1
        ORDER BY 2 DESC, 1
        LIMIT :maks
        """,
        mapOf(
            "ingenMelding" to INGEN_FEILMELDING,
            "tallMoenster" to TALL_MOENSTER,
            "maske" to MASKE,
            "maksLengde" to MAKS_LENGDE_FEILMELDING,
            "maks" to maks
        )
    ) { rs, _ ->
        FeilmeldingGruppe(
            melding = rs.getString("melding"),
            antall = rs.getLong("antall"),
            eldste = rs.getLocalDate("eldste"),
        )
    }

    fun mottak(): Mottak = jdbc.queryForObject(
        """
        SELECT (SELECT COUNT(*) FROM faktura_mottak_feil) AS antall_feil,
               (SELECT MAX(opprettet_tidspunkt) FROM faktura_mottak_feil) AS siste_feil,
               (SELECT MAX(opprettet_tidspunkt) FROM ekstern_faktura_status) AS siste_statusmelding,
               (SELECT COUNT(*) FROM ekstern_faktura_status
                 WHERE status = 'MANGLENDE_INNBETALING' AND sendt IS NOT TRUE) AS manglende_ikke_meldt
        """,
        emptyMap<String, Any>()
    ) { rs, _ ->
        Mottak(
            antallMottaksfeil = rs.getLong("antall_feil"),
            sisteMottaksfeil = rs.getInstant("siste_feil"),
            sisteStatusmelding = rs.getInstant("siste_statusmelding"),
            manglendeInnbetalingIkkeMeldt = rs.getLong("manglende_ikke_meldt"),
        )
    }!!

    fun fakturaserierGruppert(fra: Instant, til: Instant): List<FakturaserieGruppe> = jdbc.query(
        """
        SELECT fs.status::text AS status,
               fs.faktura_gjelder_innbetalingstype::text AS innbetalingstype,
               fs.intervall::text AS intervall,
               $KILDE AS kilde,
               $HENDELSE AS hendelse,
               $FULLMEKTIG AS fullmektig,
               COUNT(*) AS antall
        FROM fakturaserie fs
        WHERE fs.opprettet_tidspunkt >= :fra AND fs.opprettet_tidspunkt < :til
        GROUP BY 1, 2, 3, 4, 5, 6
        """,
        mapOf("fra" to utc(fra), "til" to utc(til))
    ) { rs, _ ->
        FakturaserieGruppe(
            status = rs.getString("status"),
            innbetalingstype = rs.getString("innbetalingstype"),
            intervall = rs.getString("intervall"),
            kilde = rs.getString("kilde"),
            hendelse = rs.getString("hendelse"),
            fullmektig = rs.getString("fullmektig"),
            antall = rs.getLong("antall"),
        )
    }

    fun antallUnikePersonerFakturaserier(fra: Instant, til: Instant): Long = jdbc.queryForObject(
        """
        SELECT COUNT(DISTINCT fs.fodselsnummer)
        FROM fakturaserie fs
        WHERE fs.opprettet_tidspunkt >= :fra AND fs.opprettet_tidspunkt < :til
        """,
        mapOf("fra" to utc(fra), "til" to utc(til)),
        Long::class.java
    ) ?: 0

    /** Fakturaer per bestillingsmåned i [[fra], [til]), både sendte og planlagte. */
    fun fakturaerPerMaaned(fra: LocalDate, til: LocalDate): List<FakturaMaaned> = jdbc.query(
        """
        WITH $FAKTURA_TOTAL
        SELECT to_char(ft.dato_bestilt, 'YYYY-MM') AS maaned,
               COUNT(*) FILTER (WHERE ft.sendt) AS antall_bestilt,
               COALESCE(SUM(ft.total) FILTER (WHERE ft.sendt), 0) AS beloep_bestilt,
               COUNT(*) FILTER (WHERE ft.status = 'OPPRETTET') AS antall_planlagt,
               COALESCE(SUM(ft.total) FILTER (WHERE ft.status = 'OPPRETTET'), 0) AS beloep_planlagt
        FROM faktura_total ft
        WHERE ft.dato_bestilt >= :fra AND ft.dato_bestilt < :til
        GROUP BY 1
        """,
        mapOf("fra" to fra, "til" to til)
    ) { rs, _ ->
        FakturaMaaned(
            maaned = rs.getString("maaned"),
            antallBestilt = rs.getLong("antall_bestilt"),
            beloepBestilt = rs.getBigDecimal("beloep_bestilt"),
            antallPlanlagt = rs.getLong("antall_planlagt"),
            beloepPlanlagt = rs.getBigDecimal("beloep_planlagt"),
        )
    }

    /** Fakturaserier opprettet per måned i [[fra], [til]). Nøkkel er «YYYY-MM». */
    fun fakturaserierPerMaaned(fra: Instant, til: Instant): Map<String, Long> = jdbc.query(
        """
        SELECT to_char(fs.opprettet_tidspunkt, 'YYYY-MM') AS maaned, COUNT(*) AS antall
        FROM fakturaserie fs
        WHERE fs.opprettet_tidspunkt >= :fra AND fs.opprettet_tidspunkt < :til
        GROUP BY 1
        """,
        mapOf("fra" to utc(fra), "til" to utc(til))
    ) { rs, _ -> rs.getString("maaned") to rs.getLong("antall") }.toMap()

    /**
     * Personer med planlagte trygdeavgiftsfakturaer i mer enn én fakturaserie, der seriene har
     * overlappende periode. Én rad per person, med referansene til seriene som overlapper – uten
     * fødselsnummeret.
     */
    fun muligDobbeltfakturering(): List<List<String>> = jdbc.query(
        """
        WITH aktive AS (
            SELECT DISTINCT fs.id, fs.referanse, fs.fodselsnummer, fs.startdato, fs.sluttdato
            FROM fakturaserie fs
            JOIN faktura f ON f.fakturaserie_id = fs.id
            WHERE f.status = 'OPPRETTET' AND fs.faktura_gjelder_innbetalingstype = 'TRYGDEAVGIFT'
        ),
        par AS (
            SELECT a.fodselsnummer, a.referanse AS ra, b.referanse AS rb
            FROM aktive a
            JOIN aktive b ON a.fodselsnummer = b.fodselsnummer AND a.id < b.id
             AND a.startdato <= b.sluttdato AND b.startdato <= a.sluttdato
        )
        SELECT string_agg(DISTINCT x.referanse, ',' ORDER BY x.referanse) AS referanser
        FROM (SELECT fodselsnummer, ra AS referanse FROM par
              UNION SELECT fodselsnummer, rb FROM par) x
        GROUP BY x.fodselsnummer
        ORDER BY 1
        """,
        emptyMap<String, Any>()
    ) { rs, _ -> rs.getString("referanser").split(",") }

    private fun tilFakturaOppfoelging(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rad: Int) = FakturaOppfoelging(
        fakturaReferanse = rs.getString("referanse_nr"),
        fakturaserieReferanse = rs.getString("fakturaserie_referanse"),
        datoBestilt = rs.getLocalDate("dato_bestilt")!!,
        sistEndret = rs.getInstant("sist_endret")!!,
    )

    private fun ResultSet.getLocalDate(kolonne: String): LocalDate? = getObject(kolonne, LocalDate::class.java)

    // Hibernate lagrer Instant som UTC-veggtid i timestamp-kolonnene, så tidspunkter leses og sendes som UTC
    private fun ResultSet.getInstant(kolonne: String): Instant? =
        getObject(kolonne, LocalDateTime::class.java)?.toInstant(ZoneOffset.UTC)

    private fun utc(tidspunkt: Instant): LocalDateTime = LocalDateTime.ofInstant(tidspunkt, ZoneOffset.UTC)

    companion object {
        const val INGEN_FEILMELDING = "(ingen feilmelding)"
        const val MAKS_LENGDE_FEILMELDING = 300

        /** Fem eller flere sifre, eventuelt skilt med mellomrom eller punktum (fnr, orgnr, fakturanummer, datoer). */
        const val TALL_MOENSTER = """\d(?:[ .]?\d){4,}"""
        const val MASKE = "***"

        /**
         * Fakturaer med totalbeløp (sum av linjene) og utledet type:
         * - KOBLING: avregningsfaktura med beløp 0. Opprettes med status BESTILT kun for å koble en
         *   tidligere faktura til avregningen, og sendes aldri til OEBS (se AvregningsfakturaGenerator).
         * - KREDITERING: negativt beløp uten avregningskobling (kreditnota ved kansellering, årsavregning
         *   med tilbakebetaling), eller admin-kreditering (er_kreditnota).
         * - AVREGNING: avregning mot en tidligere bestilt faktura (tillegg eller fradrag).
         * - ORDINAER: alt annet – den vanlige periodiske fakturaen.
         *
         * `sendt` = fakturaen har gått til OEBS: bestilt eller senere status, og ikke en koblingsfaktura.
         * `sist_endret` er tidspunktet fakturaen sist ble endret. For en faktura i BESTILT er det i praksis
         * tidspunktet den ble (om)bestilt, siden ingenting endrer den før kvitteringen kommer.
         */
        private const val FAKTURA_TOTAL = """
        faktura_total AS (
            SELECT t.*,
                   t.status IN ('BESTILT', 'INNE_I_OEBS', 'FEIL', 'MANGLENDE_INNBETALING') AND t.type <> 'KOBLING' AS sendt
            FROM (
                SELECT f.id,
                       f.referanse_nr,
                       f.fakturaserie_id,
                       f.status::text AS status,
                       f.dato_bestilt,
                       COALESCE(f.endret_tidspunkt, f.opprettet_tidspunkt) AS sist_endret,
                       COALESCE(l.total, 0) AS total,
                       CASE
                           WHEN f.avregning_faktura_id IS NOT NULL AND COALESCE(l.total, 0) = 0 THEN 'KOBLING'
                           WHEN COALESCE(l.total, 0) < 0 AND (f.avregning_faktura_id IS NULL OR f.er_kreditnota) THEN 'KREDITERING'
                           WHEN f.avregning_faktura_id IS NOT NULL THEN 'AVREGNING'
                           ELSE 'ORDINAER'
                       END AS type
                FROM faktura f
                LEFT JOIN (SELECT faktura_id, SUM(belop) AS total FROM faktura_linje GROUP BY faktura_id) l
                       ON l.faktura_id = f.id
            ) t
        )
        """

        /** Sendt til OEBS, men uten kvittering ennå. */
        private const val VENTER = "ft.status = 'BESTILT' AND ft.type <> 'KOBLING'"

        /**
         * Hvilken flyt i Melosys som opprettet serien, utledet fra referanse_bruker (teksten på fakturaen
         * til bruker). Tekstene settes i melosys-api; ukjente tekster havner i ANNET.
         */
        private const val KILDE = """
            CASE
                WHEN fs.referanse_bruker LIKE 'Vedtak om medlemskap%' THEN 'VEDTAK'
                WHEN fs.referanse_bruker LIKE 'Informasjon om trygdeavgift%' THEN 'EOS_PENSJONIST'
                WHEN fs.referanse_bruker LIKE 'Faktura for årlig satsoppdatering%' THEN 'SATSENDRING'
                WHEN fs.referanse_bruker LIKE 'Årsavregning%' THEN 'AARSAVREGNING'
                ELSE 'ANNET'
            END
        """

        /** Hvordan serien oppstod: ny, som erstatning for en tidligere serie, eller som kreditnota ved kansellering. */
        private const val HENDELSE = """
            CASE
                WHEN EXISTS (SELECT 1 FROM fakturaserie p WHERE p.erstattet_med = fs.id AND p.status = 'KANSELLERT') THEN 'KANSELLERING'
                WHEN EXISTS (SELECT 1 FROM fakturaserie p WHERE p.erstattet_med = fs.id) THEN 'ENDRING'
                ELSE 'NY'
            END
        """

        private const val FULLMEKTIG = """
            CASE
                WHEN NULLIF(fs.fullmektig_organisasjonsnummer, '') IS NOT NULL THEN 'ORGANISASJON'
                WHEN NULLIF(fs.fullmektig_fodselsnummer, '') IS NOT NULL THEN 'PERSON'
                ELSE 'INGEN'
            END
        """
    }
}

data class BestiltGruppe(
    val status: String,
    val type: String,
    val innbetalingstype: String,
    val antall: Long,
    val sumDebet: BigDecimal,
    val sumKredit: BigDecimal,
    val ikkeBetalt: Long,
    val delvisBetalt: Long,
    val sumUbetalt: BigDecimal,
)

data class Naatilstand(
    val forfalt: Long,
    val eldsteForfalt: LocalDate?,
    val venter: Long,
    val venterUnder3Dager: Long,
    val venter3Til7Dager: Long,
    val venter7Til30Dager: Long,
    val venterOver30Dager: Long,
    val eldsteVenter: Instant?,
    val koblingsfakturaer: Long,
    val planlagt: Long,
    val planlagtBeloep: BigDecimal,
    val planlagtNeste30Dager: Long,
    val planlagtNeste30DagerBeloep: BigDecimal,
    val serierMedPlanlagte: Long,
    val personerMedPlanlagte: Long,
    val feil: Long,
    val manglendeInnbetaling: Long,
)

data class FakturaOppfoelging(
    val fakturaReferanse: String,
    val fakturaserieReferanse: String,
    val datoBestilt: LocalDate,
    val sistEndret: Instant,
)

data class FeilmeldingGruppe(
    val melding: String,
    val antall: Long,
    val eldste: LocalDate?,
)

data class Mottak(
    val antallMottaksfeil: Long,
    val sisteMottaksfeil: Instant?,
    val sisteStatusmelding: Instant?,
    val manglendeInnbetalingIkkeMeldt: Long,
)

data class FakturaserieGruppe(
    val status: String,
    val innbetalingstype: String,
    val intervall: String,
    val kilde: String,
    val hendelse: String,
    val fullmektig: String,
    val antall: Long,
)

data class FakturaMaaned(
    val maaned: String,
    val antallBestilt: Long,
    val beloepBestilt: BigDecimal,
    val antallPlanlagt: Long,
    val beloepPlanlagt: BigDecimal,
)
