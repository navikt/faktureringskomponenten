package no.nav.faktureringskomponenten.controller.dto

import no.nav.faktureringskomponenten.domain.models.FaktureringsTestDsl
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype

/**
 * Oppretter en FakturaserieRequestDto med fornuftige standardverdier for alle påkrevde felt.
 * Overstyr kun de feltene du trenger for din spesifikke test.
 *
 * Eksempel:
 * ```
 * val requestDto = FakturaserieRequestDto.forTest {
 *     intervall = FakturaserieIntervall.KVARTAL
 *     periode {
 *         fra = "2024-01-01"
 *         til = "2024-03-31"
 *         månedspris = 1000
 *     }
 *     periode {
 *         fra = "2024-04-01"
 *         til = "2024-06-30"
 *         månedspris = 2000
 *     }
 * }
 * ```
 */
fun FakturaserieRequestDto.Companion.forTest(init: FakturaserieRequestDtoTestFactory.Builder.() -> Unit = {}): FakturaserieRequestDto =
    FakturaserieRequestDtoTestFactory.Builder().apply(init).build()

object FakturaserieRequestDtoTestFactory {
    const val FODSELSNUMMER = "12345678911"
    const val REFERANSE_BRUKER = "referanseBruker"
    const val REFERANSE_NAV = "referanseNav"
    val INNBETALINGSTYPE = Innbetalingstype.TRYGDEAVGIFT
    val INTERVALL = FakturaserieIntervall.KVARTAL

    @FaktureringsTestDsl
    class Builder(
        var fodselsnummer: String = FODSELSNUMMER,
        var fakturaserieReferanse: String? = null,
        var fullmektig: FullmektigDto? = null,
        var referanseBruker: String = REFERANSE_BRUKER,
        var referanseNAV: String = REFERANSE_NAV,
        var fakturaGjelderInnbetalingstype: Innbetalingstype = INNBETALINGSTYPE,
        var intervall: FakturaserieIntervall = INTERVALL,
        var perioder: MutableList<FakturaseriePeriodeDto> = mutableListOf()
    ) {
        /**
         * Legger til en ny periode ved hjelp av DSL.
         */
        fun periode(init: FakturaseriePeriodeDtoTestFactory.Builder.() -> Unit) {
            this.perioder.add(FakturaseriePeriodeDto.forTest(init))
        }

        /**
         * Legger til en eksisterende periode.
         */
        fun leggTilPeriode(periode: FakturaseriePeriodeDto) {
            this.perioder.add(periode)
        }

        /**
         * Hjelpemetode for å sette fullmektig.
         */
        fun medFullmektig(
            fodselsnummer: String? = null,
            organisasjonsnummer: String? = null
        ) {
            this.fullmektig = FullmektigDto(
                fodselsnummer = fodselsnummer,
                organisasjonsnummer = organisasjonsnummer
            )
        }

        fun build(): FakturaserieRequestDto {
            return FakturaserieRequestDto(
                fodselsnummer = fodselsnummer,
                fakturaserieReferanse = fakturaserieReferanse,
                fullmektig = fullmektig,
                referanseBruker = referanseBruker,
                referanseNAV = referanseNAV,
                fakturaGjelderInnbetalingstype = fakturaGjelderInnbetalingstype,
                intervall = intervall,
                perioder = perioder
            )
        }
    }
}

/**
 * Companion object for FakturaserieRequestDto må være tilgjengelig
 */
fun FakturaserieRequestDto.Companion() = FakturaserieRequestDto
