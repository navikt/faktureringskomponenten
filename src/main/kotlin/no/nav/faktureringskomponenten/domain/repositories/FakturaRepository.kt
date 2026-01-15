package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.AvstemmingCsvProjection
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface FakturaRepository : JpaRepository<Faktura, String> {

    @Query("SELECT f FROM Faktura f WHERE f.datoBestilt <= ?1 AND f.status = 'OPPRETTET'")
    fun findAllByDatoBestiltIsLessThanEqualAndStatusIsOpprettet(bestiltDato: LocalDate): List<Faktura>

    @EntityGraph(attributePaths = ["fakturaLinje"])
    fun findByFakturaserieReferanse(fakturaserieRef: String): List<Faktura>

    fun findByReferanseNr(referanseNr: String): Faktura?

    @EntityGraph(attributePaths = ["eksternFakturaStatus", "fakturaserie"])
    fun findByStatus(status: FakturaStatus): List<Faktura>

    @Query("SELECT COUNT(f) FROM Faktura f WHERE f.status = 'FEIL'")
    fun countByStatusIsFeil(): Int

    @Query(
        value = """
            SELECT DISTINCT
                fs.fullmektig_organisasjonsnummer AS fullmektigOrganisasjonsnummer,
                f.dato_bestilt AS datoBestilt,
                fl.totalbelop AS totalbelop,
                f.referanse_nr AS referanseNr,
                fs.referanse AS fakturaserieReferanse
            FROM faktura f
            JOIN fakturaserie fs ON f.fakturaserie_id = fs.id
            JOIN (
                SELECT
                    SUM(belop) AS totalbelop,
                    faktura_id
                FROM faktura_linje
                GROUP BY faktura_id
            ) fl ON f.id = fl.faktura_id
            WHERE f.status IN ('BESTILT', 'MANGLENDE_INNBETALING')
              AND f.dato_bestilt >= :periodeFra
              AND f.dato_bestilt <= :periodeTil
        """,
        nativeQuery = true
    )
    fun hentAvstemmingData(
        @Param("periodeFra") periodeFra: LocalDate,
        @Param("periodeTil") periodeTil: LocalDate
    ): List<AvstemmingCsvProjection>
}
