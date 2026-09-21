package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltLinjeDto
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.*

class FakturaBestiltDtoMapper {

    fun tilFakturaBestiltDto(faktura: Faktura, fakturaserie: Fakturaserie, kanselleringBeskrivelse: String? = null): FakturaBestiltDto {
        return FakturaBestiltDto(
            fodselsnummer = fakturaserie.fodselsnummer,
            fullmektigOrgnr = fakturaserie.fullmektig?.organisasjonsnummer,
            fullmektigFnr = fakturaserie.fullmektig?.fodselsnummer,
            fakturaserieReferanse = fakturaserie.referanse,
            fakturaReferanseNr = faktura.referanseNr,
            krediteringFakturaRef = faktura.krediteringFakturaRef,
            referanseBruker = fakturaserie.referanseBruker,
            referanseNAV = fakturaserie.referanseNAV,
            beskrivelse = utledBeskrivelse(faktura, fakturaserie, kanselleringBeskrivelse),
            artikkel = utledArtikkel(fakturaserie),
            faktureringsDato = LocalDate.now(),
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

    companion object {
        const val AVGIFT_TIL_FOLKETRYGDEN: String = "F00008"

        fun utledBeskrivelse(
            faktura: Faktura,
            fakturaserie: Fakturaserie,
            kanselleringBeskrivelse: String? = null
        ): String =
            utledBeskrivelse(
                fakturaserie.fakturaGjelderInnbetalingstype,
                faktura.fakturaLinje,
                fakturaserie.intervall,
                faktura.erAvregningsfaktura(),
                kanselleringBeskrivelse
            )

        fun utledArtikkel(fakturaserie: Fakturaserie): String =
            when (fakturaserie.fakturaGjelderInnbetalingstype) {
                Innbetalingstype.TRYGDEAVGIFT -> AVGIFT_TIL_FOLKETRYGDEN
                Innbetalingstype.AARSAVREGNING -> AVGIFT_TIL_FOLKETRYGDEN
            }

        private fun utledBeskrivelse(
            fakturaGjelder: Innbetalingstype,
            fakturalinjer: List<FakturaLinje>,
            intervall: FakturaserieIntervall,
            erAvregning: Boolean,
            kanselleringBeskrivelse: String? = null
        ): String {
            if (kanselleringBeskrivelse != null) {
                return kanselleringBeskrivelse
            }

            return when (fakturaGjelder) {
                Innbetalingstype.AARSAVREGNING -> {
                    return "Oppgjør av trygdeavgift for ${fakturalinjer.first().periodeFra.year}"
                }

                Innbetalingstype.TRYGDEAVGIFT -> {
                    if (erAvregning) {
                        return "Avregning mot tidligere fakturert trygdeavgift"
                    }

                    val startDatoForPerioder = fakturalinjer.minByOrNull { it.periodeFra }!!.periodeFra
                    val sluttDatoForPerioder = fakturalinjer.maxByOrNull { it.periodeTil }!!.periodeTil
                    if (intervall == FakturaserieIntervall.KVARTAL) {
                        val nåværendeKvartal = startDatoForPerioder[IsoFields.QUARTER_OF_YEAR]
                        val sluttKvartal = sluttDatoForPerioder[IsoFields.QUARTER_OF_YEAR]
                        if (nåværendeKvartal < sluttKvartal) "Trygdeavgift $nåværendeKvartal.kvartal ${startDatoForPerioder.year} - $sluttKvartal.kvartal ${sluttDatoForPerioder.year}"
                        else "Trygdeavgift $nåværendeKvartal. kvartal ${startDatoForPerioder.year}"
                    } else {
                        val nåværendeMåned =
                            startDatoForPerioder.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        "Trygdeavgift $nåværendeMåned ${startDatoForPerioder.year}"
                    }
                }
            }
        }
    }
}
