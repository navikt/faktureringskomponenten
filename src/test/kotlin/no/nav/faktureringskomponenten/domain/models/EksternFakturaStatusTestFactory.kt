package no.nav.faktureringskomponenten.domain.models

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Oppretter en EksternFakturaStatus med fornuftige standardverdier for alle påkrevde felt.
 * Overstyr kun de feltene du trenger for din spesifikke test.
 *
 * Eksempel:
 * ```
 * val eksternStatus = EksternFakturaStatus.forTest {
 *     status = FakturaStatus.BESTILT
 *     dato = "2024-01-15"
 *     fakturaBelop = 3000
 *     ubetaltBelop = 1000
 * }
 * ```
 */
fun EksternFakturaStatus.Companion.forTest(init: EksternFakturaStatusTestFactory.Builder.() -> Unit = {}): EksternFakturaStatus =
    EksternFakturaStatusTestFactory.Builder().apply(init).build()

object EksternFakturaStatusTestFactory {
    val DEFAULT_DATO = LocalDate.of(2024, 1, 1)
    val DEFAULT_STATUS = FakturaStatus.OPPRETTET
    const val DEFAULT_FAKTURA_BELOP = 1000
    const val DEFAULT_UBETALT_BELOP = 0

    @FaktureringsTestDsl
    class Builder(
        var id: Long? = null,
        var dato: LocalDate? = DEFAULT_DATO,
        var status: FakturaStatus? = DEFAULT_STATUS,
        var fakturaBelop: BigDecimal? = BigDecimal(DEFAULT_FAKTURA_BELOP),
        var ubetaltBelop: BigDecimal? = BigDecimal(DEFAULT_UBETALT_BELOP),
        var feilMelding: String? = null,
        var sendt: Boolean? = false,
        var faktura: Faktura? = null
    ) {
        /**
         * Setter dato fra en string på formatet "yyyy-MM-dd".
         * Alias for ergonomic API
         */
        var datoString: String
            get() = dato?.toString() ?: ""
            set(value) {
                dato = LocalDate.parse(value)
            }

        /**
         * Setter fakturaBelop fra en int.
         * Alias: beløp
         */
        var beløp: Int
            get() = fakturaBelop?.toInt() ?: 0
            set(value) {
                fakturaBelop = BigDecimal(value)
            }

        /**
         * Setter ubetaltBelop fra en int.
         * Alias: ubetalt
         */
        var ubetalt: Int
            get() = ubetaltBelop?.toInt() ?: 0
            set(value) {
                ubetaltBelop = BigDecimal(value)
            }

        fun build(): EksternFakturaStatus {
            return EksternFakturaStatus(
                id = id,
                dato = dato,
                status = status,
                fakturaBelop = fakturaBelop?.setScale(2),
                ubetaltBelop = ubetaltBelop?.setScale(2),
                feilMelding = feilMelding,
                sendt = sendt,
                faktura = faktura
            )
        }
    }
}
