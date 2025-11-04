package no.nav.faktureringskomponenten.domain.models

import ulid.ULID
import java.time.LocalDate

/**
 * Oppretter en Fakturaserie med fornuftige standardverdier for alle påkrevde felt.
 * Overstyr kun de feltene du trenger for din spesifikke test.
 *
 * Eksempel:
 * ```
 * val fakturaserie = Fakturaserie.forTest {
 *     intervall = KVARTAL
 *     faktura {
 *         status = BESTILT
 *         fakturaLinje {
 *             fra = "2024-01-01"
 *             til = "2024-03-31"
 *             månedspris = 1000
 *         }
 *     }
 *     faktura {
 *         status = OPPRETTET
 *         fakturaLinje {
 *             fra = "2024-04-01"
 *             til = "2024-06-30"
 *             månedspris = 2000
 *         }
 *     }
 * }
 * ```
 */
fun Fakturaserie.Companion.forTest(init: FakturaserieTestFactory.Builder.() -> Unit = {}): Fakturaserie =
    FakturaserieTestFactory.Builder().apply(init).build()

object FakturaserieTestFactory {
    const val REFERANSE = "MEL-TEST-123"
    const val FODSELSNUMMER = "12345678901"
    const val REFERANSE_BRUKER = "Bruker referanse"
    const val REFERANSE_NAV = "NAV referanse"
    val START_DATO = LocalDate.of(2024, 1, 1)
    val SLUTT_DATO = LocalDate.of(2024, 12, 31)
    val STATUS = FakturaserieStatus.OPPRETTET
    val INTERVALL = FakturaserieIntervall.KVARTAL
    val INNBETALINGSTYPE = Innbetalingstype.TRYGDEAVGIFT

    @FaktureringsTestDsl
    class Builder(
        var id: Long? = null,
        var referanse: String = REFERANSE + "-" + ULID.randomULID().substring(0, 8),
        var fakturaGjelderInnbetalingstype: Innbetalingstype = INNBETALINGSTYPE,
        var fodselsnummer: String = FODSELSNUMMER,
        var fullmektig: Fullmektig? = null,
        var referanseBruker: String = REFERANSE_BRUKER,
        var referanseNAV: String = REFERANSE_NAV,
        var startdato: LocalDate = START_DATO,
        var sluttdato: LocalDate = SLUTT_DATO,
        var status: FakturaserieStatus = STATUS,
        var intervall: FakturaserieIntervall = INTERVALL,
        var faktura: MutableList<Faktura> = mutableListOf(),
        var erstattetMed: Fakturaserie? = null
    ) {
        /**
         * Setter startdato fra en string på formatet "yyyy-MM-dd".
         */
        var fra: String
            get() = startdato.toString()
            set(value) {
                startdato = LocalDate.parse(value)
            }

        /**
         * Setter sluttdato fra en string på formatet "yyyy-MM-dd".
         */
        var til: String
            get() = sluttdato.toString()
            set(value) {
                sluttdato = LocalDate.parse(value)
            }

        /**
         * Legger til en ny faktura ved hjelp av DSL.
         */
        fun faktura(init: FakturaTestFactory.Builder.() -> Unit) {
            this.faktura.add(Faktura.forTest(init))
        }

        /**
         * Legger til en eksisterende faktura.
         */
        fun leggTilFaktura(faktura: Faktura) {
            this.faktura.add(faktura)
        }

        /**
         * Hjelpemetode for å sette fullmektig.
         */
        fun medFullmektig(
            fodselsnummer: String? = null,
            organisasjonsnummer: String? = null
        ) {
            this.fullmektig = Fullmektig(
                fodselsnummer = fodselsnummer,
                organisasjonsnummer = organisasjonsnummer
            )
        }

        fun build(): Fakturaserie {
            val fakturaserie = Fakturaserie(
                id = id,
                referanse = referanse,
                fakturaGjelderInnbetalingstype = fakturaGjelderInnbetalingstype,
                fodselsnummer = fodselsnummer,
                fullmektig = fullmektig,
                referanseBruker = referanseBruker,
                referanseNAV = referanseNAV,
                startdato = startdato,
                sluttdato = sluttdato,
                status = status,
                intervall = intervall,
                faktura = faktura,
                erstattetMed = erstattetMed
            )
            // Wire relationships
            fakturaserie.faktura.forEach { it.fakturaserie = fakturaserie }
            return fakturaserie
        }
    }
}

/**
 * Companion object for Fakturaserie må være tilgjengelig
 */
fun Fakturaserie.Companion() = Fakturaserie
