package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltLinjeDto
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.*

class FakturaBestiltDtoMapper {
    val AVGIFT_TIL_FOLKETRYGDEN: String = "F00008"
    val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun tilFakturaBestiltDto(faktura: Faktura, fakturaserie: Fakturaserie): FakturaBestiltDto {
        return FakturaBestiltDto(
            fodselsnummer = fakturaserie.fodselsnummer,
            fullmektigOrgnr = fakturaserie.fullmektig?.organisasjonsnummer,
            fullmektigFnr = fakturaserie.fullmektig?.fodselsnummer,
            fakturaserieReferanse = fakturaserie.referanse,
            fakturaReferanseNr = faktura.referanseNr,
            kreditReferanseNr = faktura.kreditReferanseNr,
            referanseBruker = fakturaserie.referanseBruker,
            referanseNAV = fakturaserie.referanseNAV,
            beskrivelse = mapFakturaBeskrivelse(
                fakturaserie.fakturaGjelderInnbetalingstype,
                faktura.fakturaLinje,
                fakturaserie.intervall,
                faktura.erAvregningsfaktura()
            ),
            artikkel = mapArtikkel(fakturaserie.fakturaGjelderInnbetalingstype),
            faktureringsDato = faktura.datoBestilt,
            fakturaLinjer = faktura.fakturaLinje.map {
                FakturaBestiltLinjeDto(
                    beskrivelse = it.beskrivelse,
                    antall = it.antall,
                    enhetspris = it.enhetsprisPerManed,
                    belop = it.belop
                )
            }
        )
    }

    private fun mapFakturaBeskrivelse(
        fakturaGjelder: Innbetalingstype,
        fakturalinjer: List<FakturaLinje>,
        intervall: FakturaserieIntervall,
        erAvregning: Boolean
    ): String {
        return when (fakturaGjelder) {
            Innbetalingstype.TRYGDEAVGIFT -> {
                if (erAvregning) {
                    return "Faktura for avregning mot tidligere fakturert trygdeavgift"
                }

                val startDatoForPerioder = fakturalinjer.minByOrNull { it.periodeFra }!!.periodeFra
                val sluttDatoForPerioder = fakturalinjer.maxByOrNull { it.periodeTil }!!.periodeTil
                if (intervall == FakturaserieIntervall.KVARTAL) {
                    val nåværendeKvartal = startDatoForPerioder[IsoFields.QUARTER_OF_YEAR]
                    val sluttKvartal = sluttDatoForPerioder[IsoFields.QUARTER_OF_YEAR]
                    if (nåværendeKvartal < sluttKvartal) "Faktura Trygdeavgift $nåværendeKvartal.kvartal ${startDatoForPerioder.year} - $sluttKvartal.kvartal ${sluttDatoForPerioder.year}"
                    else "Faktura Trygdeavgift $nåværendeKvartal. kvartal ${startDatoForPerioder.year}"
                } else {
                    val nåværendeMåned = startDatoForPerioder.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    "Faktura Trygdeavgift $nåværendeMåned ${startDatoForPerioder.year}"
                }
            }
        }
    }

    private fun mapArtikkel(fakturaGjelder: Innbetalingstype): String {
        return when (fakturaGjelder) {
            Innbetalingstype.TRYGDEAVGIFT -> AVGIFT_TIL_FOLKETRYGDEN
        }
    }
}
