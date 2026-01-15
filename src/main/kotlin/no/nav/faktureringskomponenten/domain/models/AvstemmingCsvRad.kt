package no.nav.faktureringskomponenten.domain.models

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.math.BigDecimal
import java.time.LocalDate

interface AvstemmingCsvProjection {
    fun getFullmektigOrganisasjonsnummer(): String?
    fun getDatoBestilt(): LocalDate
    fun getTotalbelop(): BigDecimal
    fun getReferanseNr(): String
    fun getFakturaserieReferanse(): String
}

@JsonPropertyOrder(
    "fullmektigOrganisasjonsnummer",
    "datoBestilt",
    "totalbelop",
    "referanseNr",
    "fakturaserieReferanse"
)
data class AvstemmingCsvRad(
    @field:JsonProperty("Fullmektig Organisasjonsnummer")
    val fullmektigOrganisasjonsnummer: String?,

    @field:JsonProperty("Dato Bestilt")
    @field:JsonFormat(pattern = "yyyy-MM-dd")
    val datoBestilt: LocalDate,

    @field:JsonProperty("Totalbeløp")
    val totalbelop: BigDecimal,

    @field:JsonProperty("Faktura Referanse")
    val referanseNr: String,

    @field:JsonProperty("Fakturaserie Referanse")
    val fakturaserieReferanse: String
) {
    companion object {
        fun fra(projection: AvstemmingCsvProjection) = AvstemmingCsvRad(
            fullmektigOrganisasjonsnummer = projection.getFullmektigOrganisasjonsnummer(),
            datoBestilt = projection.getDatoBestilt(),
            totalbelop = projection.getTotalbelop(),
            referanseNr = projection.getReferanseNr(),
            fakturaserieReferanse = projection.getFakturaserieReferanse()
        )
    }
}
