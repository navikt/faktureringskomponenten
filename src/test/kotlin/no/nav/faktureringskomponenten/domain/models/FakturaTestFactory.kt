package no.nav.faktureringskomponenten.domain.models

import ulid.ULID
import java.time.LocalDate

/**
 * Oppretter en Faktura med fornuftige standardverdier for alle påkrevde felt.
 * Overstyr kun de feltene du trenger for din spesifikke test.
 *
 * Eksempel:
 * ```
 * val faktura = Faktura.forTest {
 *     status = BESTILT
 *     fakturaserie {
 *         referanse = "MEL-123"
 *     }
 *     fakturaLinje {
 *         fra = "2024-01-01"
 *         til = "2024-03-31"
 *         månedspris = 1000
 *     }
 *     fakturaLinje {
 *         fra = "2024-04-01"
 *         til = "2024-06-30"
 *         månedspris = 2000
 *     }
 * }
 * ```
 */
fun Faktura.Companion.forTest(init: FakturaTestFactory.Builder.() -> Unit = {}): Faktura =
    FakturaTestFactory.Builder().apply(init).build()

object FakturaTestFactory {
    val DATO_BESTILT = LocalDate.of(2024, 1, 1)
    val STATUS = FakturaStatus.OPPRETTET
    const val EKSTERN_FAKTURA_NUMMER = "TEST-123"

    @FaktureringsTestDsl
    class Builder(
        var id: Long? = null,
        var referanseNr: String = ULID.randomULID(),
        var datoBestilt: LocalDate = DATO_BESTILT,
        var status: FakturaStatus = STATUS,
        var fakturaLinje: MutableList<FakturaLinje> = mutableListOf(),
        var fakturaserie: Fakturaserie? = null,
        var eksternFakturaStatus: MutableList<EksternFakturaStatus> = mutableListOf(),
        var eksternFakturaNummer: String = EKSTERN_FAKTURA_NUMMER,
        var krediteringFakturaRef: String = "",
        var referertFakturaVedAvregning: Faktura? = null
    ) {
        /**
         * Setter datoBestilt fra en string på formatet "yyyy-MM-dd".
         */
        var bestilt: String
            get() = datoBestilt.toString()
            set(value) {
                datoBestilt = LocalDate.parse(value)
            }

        /**
         * Legger til en ny fakturalinje ved hjelp av DSL.
         */
        fun fakturaLinje(init: FakturaLinjeTestFactory.Builder.() -> Unit) {
            this.fakturaLinje.add(FakturaLinje.forTest(init))
        }

        /**
         * Legger til en eksisterende fakturalinje.
         */
        fun leggTilFakturaLinje(linje: FakturaLinje) {
            this.fakturaLinje.add(linje)
        }

        /**
         * Setter fakturaserie ved hjelp av DSL.
         */
        fun fakturaserie(init: FakturaserieTestFactory.Builder.() -> Unit) {
            this.fakturaserie = Fakturaserie.forTest(init)
        }

        fun build(): Faktura {
            val faktura = Faktura(
                id = id,
                referanseNr = referanseNr,
                datoBestilt = datoBestilt,
                status = status,
                fakturaLinje = fakturaLinje,
                fakturaserie = fakturaserie,
                eksternFakturaStatus = eksternFakturaStatus,
                eksternFakturaNummer = eksternFakturaNummer,
                krediteringFakturaRef = krediteringFakturaRef,
                referertFakturaVedAvregning = referertFakturaVedAvregning
            )
            // Wire relationships: fakturaLinje får ikke automatisk faktura-referanse i constructor
            // Dette må gjøres manuelt
            return faktura
        }
    }

    /**
     * Builder-metoder for Java-kompatibilitet
     */
    @JvmStatic
    fun builder() = Builder()

    @JvmStatic
    fun lagFaktura() = builder().build()

    /**
     * Hjelpemetode for å lage en faktura med bestilt status.
     */
    @JvmStatic
    fun lagBestiltFaktura(
        periodeFra: LocalDate = LocalDate.of(2024, 1, 1),
        periodeTil: LocalDate = LocalDate.of(2024, 3, 31),
        enhetspris: Int = 1000
    ): Faktura = Faktura.forTest {
        status = FakturaStatus.BESTILT
        fakturaLinje {
            fra = periodeFra.toString()
            til = periodeTil.toString()
            månedspris = enhetspris
        }
    }
}

/**
 * Companion object for Faktura må være tilgjengelig
 */
fun Faktura.Companion() = Faktura
