package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltLinjeDto
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.*

@Component
class FakturaBestiltDtoMapper {
    val AVGIFT_TIL_FOLKETRYGDEN: String = "F00008"
    val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun tilFakturaBestiltDto(faktura: Faktura, fakturaserie: Fakturaserie): FakturaBestiltDto {
        return FakturaBestiltDto(
            fodselsnummer = fakturaserie.fodselsnummer,
            fullmektigOrgnr = fakturaserie.fullmektig?.organisasjonsnummer,
            fullmektigFnr = fakturaserie.fullmektig?.fodselsnummer,
            vedtaksId = fakturaserie.vedtaksId,
            fakturaReferanseNr = "${faktura.id}",
            kreditReferanseNr = "",
            referanseBruker = fakturaserie.referanseBruker,
            referanseNAV = fakturaserie.referanseNAV,
            beskrivelse = mapFakturaBeskrivelse(fakturaserie.fakturaGjelder, fakturaserie.intervall),
            artikkel = mapArtikkel(fakturaserie.fakturaGjelder),
            faktureringsDato = faktura.datoBestilt,
            fakturaLinjer = faktura.fakturaLinje.map {
                FakturaBestiltLinjeDto(
                    beskrivelse = mapFakturaserieBeskrivelse(it),
                    antall = 1.0,
                    enhetspris = it.enhetsprisPerManed,
                    belop = it.belop
                )
            }
        )
    }

    private fun mapFakturaserieBeskrivelse(fakturaLinje: FakturaLinje): String {
        val periodeFraFormatert = fakturaLinje.periodeFra.format(FORMATTER)
        val periodeTilFormatert = fakturaLinje.periodeTil.format(FORMATTER)

        return "Periode: $periodeFraFormatert - ${periodeTilFormatert}, ${fakturaLinje.beskrivelse}"
    }


    private fun mapFakturaBeskrivelse(fakturaGjelder: FakturaGjelder, intervall: FakturaserieIntervall): String {
        return when (fakturaGjelder) {
            FakturaGjelder.TRYGDEAVGIFT -> {
                if (intervall == FakturaserieIntervall.KVARTAL) {
                    val nåværendeKvartal = LocalDate.now().get(IsoFields.QUARTER_OF_YEAR)
                    val nåværendeÅr = LocalDate.now().year
                    "Faktura Trygdeavgift $nåværendeKvartal. kvartal $nåværendeÅr"
                } else {
                    val nåværendeMåned = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    val nåværendeÅr = LocalDate.now().year
                    "Faktura Trygdeavgift $nåværendeMåned $nåværendeÅr"
                }
            }
        }
    }

    private fun mapArtikkel(fakturaGjelder: FakturaGjelder): String {
        return when (fakturaGjelder) {
            FakturaGjelder.TRYGDEAVGIFT -> AVGIFT_TIL_FOLKETRYGDEN
        }
    }
}
