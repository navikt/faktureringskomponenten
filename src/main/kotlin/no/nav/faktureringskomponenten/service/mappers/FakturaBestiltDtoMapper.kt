package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltLinjeDto
import java.time.LocalDate
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
            kreditReferanseNr = "",
            referanseBruker = fakturaserie.referanseBruker,
            referanseNAV = fakturaserie.referanseNAV,
            beskrivelse = mapFakturaBeskrivelse(fakturaserie.fakturaGjelderInnbetalingstype, fakturaserie.intervall, faktura.erAvregningsfaktura()),
            artikkel = mapArtikkel(fakturaserie.fakturaGjelderInnbetalingstype),
            faktureringsDato = faktura.datoBestilt,
            fakturaLinjer = faktura.fakturaLinje.map {
                FakturaBestiltLinjeDto(
                    beskrivelse = lagBestiltLinjeBeskrivelse(it, faktura.erAvregningsfaktura()),
                    antall = it.antall,
                    enhetspris = it.enhetsprisPerManed,
                    belop = it.belop
                )
            }
        )
    }

    private fun lagBestiltLinjeBeskrivelse(fakturaLinje: FakturaLinje, erAvregning: Boolean): String {
        val prefiks = if (erAvregning) "Avregning mot fakturanummer ${fakturaLinje.referertFakturaVedAvregning?.id}, " else ""

        val periodeFraFormatert = fakturaLinje.periodeFra.format(FORMATTER)
        val periodeTilFormatert = fakturaLinje.periodeTil.format(FORMATTER)

        return prefiks + "Periode: $periodeFraFormatert - ${periodeTilFormatert}, ${fakturaLinje.beskrivelse}"
    }


    private fun mapFakturaBeskrivelse(fakturaGjelder: Innbetalingstype, intervall: FakturaserieIntervall, erAvregning: Boolean): String {
        return when (fakturaGjelder) {
            Innbetalingstype.TRYGDEAVGIFT -> {
                if (erAvregning) {
                    return "Faktura for avregning mot tidligere fakturert trygdeavgift"
                }

                val nå = LocalDate.now()
                if (intervall == FakturaserieIntervall.KVARTAL) {
                    val nåværendeKvartal = nå[IsoFields.QUARTER_OF_YEAR]
                    "Faktura Trygdeavgift $nåværendeKvartal. kvartal ${nå.year}"
                } else {
                    val nåværendeMåned = nå.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    "Faktura Trygdeavgift $nåværendeMåned ${nå.year}"
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
