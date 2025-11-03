package no.nav.faktureringskomponenten.controller.dto

import no.nav.faktureringskomponenten.domain.models.FaktureringsTestDsl
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Oppretter en FakturaseriePeriodeDto med fornuftige standardverdier for alle påkrevde felt.
 * Overstyr kun de feltene du trenger for din spesifikke test.
 *
 * Eksempel:
 * ```
 * val periodeDto = FakturaseriePeriodeDto.forTest {
 *     fra = "2024-01-01"
 *     til = "2024-03-31"
 *     månedspris = 1000
 * }
 * ```
 */
fun FakturaseriePeriodeDto.Companion.forTest(init: FakturaseriePeriodeDtoTestFactory.Builder.() -> Unit = {}): FakturaseriePeriodeDto =
    FakturaseriePeriodeDtoTestFactory.Builder().apply(init).build()

object FakturaseriePeriodeDtoTestFactory {
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

        fun build(): FakturaseriePeriodeDto {
            return FakturaseriePeriodeDto(
                enhetsprisPerManed = enhetsprisPerManed,
                startDato = startDato,
                sluttDato = sluttDato,
                beskrivelse = beskrivelse
            )
        }
    }
}

/**
 * Companion object for FakturaseriePeriodeDto må være tilgjengelig
 */
fun FakturaseriePeriodeDto.Companion() = FakturaseriePeriodeDto
