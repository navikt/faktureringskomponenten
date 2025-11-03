package no.nav.faktureringskomponenten.domain.models

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Oppretter en FakturaLinje med fornuftige standardverdier for alle påkrevde felt.
 * Overstyr kun de feltene du trenger for din spesifikke test.
 *
 * Eksempel:
 * ```
 * val fakturaLinje = FakturaLinje.forTest {
 *     fra = "2024-01-01"
 *     til = "2024-03-31"
 *     månedspris = 1000
 * }
 * ```
 */
fun FakturaLinje.Companion.forTest(init: FakturaLinjeTestFactory.Builder.() -> Unit = {}): FakturaLinje =
    FakturaLinjeTestFactory.Builder().apply(init).build()

object FakturaLinjeTestFactory {
    val PERIODE_FRA = LocalDate.of(2024, 1, 1)
    val PERIODE_TIL = LocalDate.of(2024, 3, 31)
    const val ENHETSPRIS_PER_MANED = 1000
    const val BESKRIVELSE = "Test fakturalinje"

    @FaktureringsTestDsl
    class Builder(
        var id: Long? = null,
        var periodeFra: LocalDate = PERIODE_FRA,
        var periodeTil: LocalDate = PERIODE_TIL,
        var beskrivelse: String = BESKRIVELSE,
        var antall: BigDecimal = BigDecimal.ONE,
        var enhetsprisPerManed: BigDecimal = BigDecimal(ENHETSPRIS_PER_MANED),
        var belop: BigDecimal? = null,
        var avregningForrigeBeloep: BigDecimal? = null,
        var avregningNyttBeloep: BigDecimal? = null
    ) {
        /**
         * Setter periodeFra fra en string på formatet "yyyy-MM-dd".
         * Alias: fra
         */
        var fra: String
            get() = periodeFra.toString()
            set(value) {
                periodeFra = LocalDate.parse(value)
            }

        /**
         * Setter periodeTil fra en string på formatet "yyyy-MM-dd".
         * Alias: til
         */
        var til: String
            get() = periodeTil.toString()
            set(value) {
                periodeTil = LocalDate.parse(value)
            }

        /**
         * Setter enhetsprisPerManed fra en int.
         * Alias: månedspris
         */
        var månedspris: Int
            get() = enhetsprisPerManed.toInt()
            set(value) {
                enhetsprisPerManed = BigDecimal(value)
            }

        fun build(): FakturaLinje {
            // Beregn beløp automatisk hvis ikke satt, med riktig skala (2 desimaler)
            val calculatedBelop = (belop ?: (enhetsprisPerManed * antall)).setScale(2)

            return FakturaLinje(
                id = id,
                periodeFra = periodeFra,
                periodeTil = periodeTil,
                beskrivelse = beskrivelse,
                antall = antall,
                enhetsprisPerManed = enhetsprisPerManed.setScale(2),
                belop = calculatedBelop,
                avregningForrigeBeloep = avregningForrigeBeloep?.setScale(2),
                avregningNyttBeloep = avregningNyttBeloep?.setScale(2)
            )
        }
    }

    /**
     * Builder-metoder for Java-kompatibilitet
     */
    @JvmStatic
    fun builder() = Builder()

    @JvmStatic
    fun lagFakturaLinje() = builder().build()
}

/**
 * Companion object for FakturaLinje må være tilgjengelig
 */
fun FakturaLinje.Companion() = FakturaLinje
