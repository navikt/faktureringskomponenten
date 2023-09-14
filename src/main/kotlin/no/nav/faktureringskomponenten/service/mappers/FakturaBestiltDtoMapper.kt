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
            fakturaserieReferanse = fakturaserie.referanse,
            fakturaReferanseNr = "${faktura.id}",
            kreditReferanseNr = "",
            referanseBruker = fakturaserie.referanseBruker,
            referanseNAV = fakturaserie.referanseNAV,
            beskrivelse = mapFakturaBeskrivelse(fakturaserie.fakturaGjelderInnbetalingstype, fakturaserie.intervall),
            artikkel = mapArtikkel(fakturaserie.fakturaGjelderInnbetalingstype),
            faktureringsDato = faktura.datoBestilt,
            fakturaLinjer = faktura.fakturaLinje.map {
                FakturaBestiltLinjeDto(
                    beskrivelse = mapFakturaserieBeskrivelse(it),
                    antall = it.antall,
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


    private fun mapFakturaBeskrivelse(fakturaGjelder: Innbetalingstype, intervall: FakturaserieIntervall): String {
        return when (fakturaGjelder) {
            Innbetalingstype.TRYGDEAVGIFT -> {
                val nå = LocalDate.now()
                if (intervall == FakturaserieIntervall.KVARTAL) {
                    val nåværendeKvartal = nå.get(IsoFields.QUARTER_OF_YEAR)
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
