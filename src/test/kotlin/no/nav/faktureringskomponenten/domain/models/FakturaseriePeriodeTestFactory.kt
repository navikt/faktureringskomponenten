package no.nav.faktureringskomponenten.domain.models

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Oppretter en FakturaseriePeriode med fornuftige standardverdier for alle påkrevde felt.
 * Overstyr kun de feltene du trenger for din spesifikke test.
 *
 * Eksempel:
 * ```
 * val periode = FakturaseriePeriode.forTest {
 *     fra = "2024-01-01"
 *     til = "2024-03-31"
 *     månedspris = 1000
 *     beskrivelse = "Inntekt: 100000, Dekning: Pensjonsdel, Sats: 10%"
 * }
 * ```
 */
fun FakturaseriePeriode.Companion.forTest(init: FakturaseriePeriodeTestFactory.Builder.() -> Unit = {}): FakturaseriePeriode =
    FakturaseriePeriodeTestFactory.Builder().apply(init).build()

object FakturaseriePeriodeTestFactory {
    val START_DATO = LocalDate.of(2024, 1, 1)
    val SLUTT_DATO = LocalDate.of(2024, 3, 31)
    const val ENHETSPRIS_PER_MANED = 1000
    const val BESKRIVELSE = "Test periode"

    @FaktureringsTestDsl
    class Builder(
        var enhetsprisPerManed: BigDecimal = BigDecimal(ENHETSPRIS_PER_MANED),
        var startDato: LocalDate = START_DATO,
        var sluttDato: LocalDate = SLUTT_DATO,
        var beskrivelse: String = BESKRIVELSE
    ) {
        /**
         * Setter startDato fra en string på formatet "yyyy-MM-dd".
         * Alias: fra
         */
        var fra: String
            get() = startDato.toString()
            set(value) {
                startDato = LocalDate.parse(value)
            }

        /**
         * Setter sluttDato fra en string på formatet "yyyy-MM-dd".
         * Alias: til
         */
        var til: String
            get() = sluttDato.toString()
            set(value) {
                sluttDato = LocalDate.parse(value)
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

        fun build(): FakturaseriePeriode {
            return FakturaseriePeriode(
                enhetsprisPerManed = enhetsprisPerManed.setScale(2),
                startDato = startDato,
                sluttDato = sluttDato,
                beskrivelse = beskrivelse
            )
        }
    }
}

/**
 * Companion object for FakturaseriePeriode må være tilgjengelig
 */
fun FakturaseriePeriode.Companion() = FakturaseriePeriode
