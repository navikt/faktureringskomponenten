package no.nav.faktureringskomponenten.controller.dto

import no.nav.faktureringskomponenten.domain.models.EksternFakturaStatus
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import java.math.BigDecimal
import java.time.LocalDate

data class EksternFakturaStatusAdminDto(
    val id: Long?,
    val dato: LocalDate?,
    val status: FakturaStatus?,
    val fakturaBelop: BigDecimal?,
    val ubetaltBelop: BigDecimal?,
    val feilMelding: String?,
    val sendt: Boolean?
)

data class FakturaAdminDto(
    val id: Long?,
    val referanseNr: String,
    val datoBestilt: LocalDate,
    val status: FakturaStatus,
    val eksternFakturaNummer: String,
    val krediteringFakturaRef: String?,

    val fakturaserieReferanse: String?,

    val eksternFakturaStatus: List<EksternFakturaStatusAdminDto> = emptyList()
)

fun EksternFakturaStatus.toEksternFakturaStatusAdminDto(): EksternFakturaStatusAdminDto {
    return EksternFakturaStatusAdminDto(
        id = this.id,
        dato = this.dato,
        status = this.status,
        fakturaBelop = this.fakturaBelop,
        ubetaltBelop = this.ubetaltBelop,
        feilMelding = this.feilMelding,
        sendt = this.sendt
    )
}

fun Faktura.toFakturaAdminDto(): FakturaAdminDto {
    return FakturaAdminDto(
        id = this.id,
        referanseNr = this.referanseNr,
        datoBestilt = this.datoBestilt,
        status = this.status,
        eksternFakturaNummer = this.eksternFakturaNummer,
        krediteringFakturaRef = this.krediteringFakturaRef,
        fakturaserieReferanse = this.fakturaserie?.referanse,
        eksternFakturaStatus = this.eksternFakturaStatus.map { it.toEksternFakturaStatusAdminDto() }
    )
}
