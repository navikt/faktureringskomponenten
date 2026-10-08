package no.nav.faktureringskomponenten.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Model for en faktura i fakturaserien")
data class FakturaResponseDto(

    @Schema(description = "Unik identifikator av faktura")
    val fakturaReferanse: String,

    @Schema(description = "Dato for når faktura bestilles til OEBS")
    val datoBestilt: LocalDate,

    @Schema(description = "Dato for når faktura sist ble oppdatert")
    val sistOppdatert: LocalDateTime,

    var status: FakturaStatus,

    @Schema(description = "Fakturalinjer i fakturaen")
    val fakturaLinje: List<FakturaLinjeResponseDto>,

    @Schema(description = "Startdato for perioden")
    val periodeFra: LocalDate,

    @Schema(description = "Sluttdato for perioden")
    val periodeTil: LocalDate,

    val eksternFakturaStatus: List<FakturaTilbakemeldingResponseDto>,

    val eksternFakturaNummer: String? = "",

    @Schema(description = "Om fakturaen er en kreditnota")
    val erKreditnota: Boolean = false,

    @Schema(description = "Fakturaens hodebeskrivelse slik den sendes til OEBS. Null for kreditnotaer som ble bestilt før beskrivelsen ble lagret, siden den ikke kan utledes på nytt")
    val beskrivelse: String?,

    @Schema(description = "Artikkelen fakturaen føres på i OEBS")
    val artikkel: String,

    @Schema(description = "True når beskrivelsen er utledet på nytt fordi fakturaen ikke er bestilt, eller ble bestilt før feltet ble lagret. Utledet beskrivelse er ikke garantert lik den OEBS fikk")
    val beskrivelseErUtledet: Boolean,

    @Schema(description = "Referansenummer til fakturaen denne krediterer")
    val krediteringFakturaRef: String? = null,

    @Schema(description = "Sum av alle fakturalinjer")
    val totalbelop: BigDecimal,
)
