package no.nav.faktureringskomponenten.domain.repositories

import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FakturaserieRepository : JpaRepository<Fakturaserie, String> {

    fun findByReferanse(referanse: String?): Fakturaserie?

    /*
    * To forskjellige Common Table Expression(CTE) her. fakturaserie_forward - henter alle "parents" av
    * relasjonen av fakturaserie. fakturaserie_reverse - henter alle "children"
    * av relasjonen. Kan se på dette som en som sjekker høyre og venstre, er det
    * noen relasjoner av meg til venstre(fakturaserie_forward) blir de funnnet av
    * denne CTE, hvis det er noen til høyre(fakturaserie_reverse) blir de funnnet
    * av denne. Så den endelige SELECT henter svar fra begge CTE og joiner faktura
    * og finner hvilke fakturaserie som har faktura med gitt status.
    * */
    @Query(value = """
    WITH RECURSIVE 
    fakturaserie_forward AS (
        SELECT fs.* FROM Fakturaserie fs
        WHERE fs.referanse = :referanse
        UNION ALL
        SELECT fs.* FROM Fakturaserie fs
        JOIN fakturaserie_forward fr ON fs.erstattet_med = fr.id
    ),
    fakturaserie_reverse AS (
        SELECT fs.* FROM Fakturaserie fs
        WHERE fs.id IN (
            SELECT DISTINCT erstattet_med FROM Fakturaserie
            WHERE referanse = :referanse
        ) 
        UNION ALL
        SELECT fs.* FROM Fakturaserie fs
        JOIN fakturaserie_reverse fr ON fs.id = fr.erstattet_med
    )
    SELECT distinct fs.* FROM fakturaserie_forward fs
    UNION ALL
    SELECT distinct fs.* FROM fakturaserie_reverse fs
    """, nativeQuery = true)
    fun findAllByReferanse(
        @Param("referanse") referanse: String,
    ): List<Fakturaserie>



    fun findById(id: Long): Fakturaserie?
}