package no.nav.faktureringskomponenten.service

import io.getunleash.Unleash
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class FakturaserieGenerator(
    @Autowired val unleash: Unleash,
    val fakturaGenerator: FakturaGenerator = FakturaGenerator(unleash = unleash)
) {

    fun lagFakturaserie(fakturaserieDto: FakturaserieDto, startDato: LocalDate? = null): Fakturaserie {
        val startDatoForHelePerioden = startDato ?: mapStartdato(fakturaserieDto.perioder)
        val sluttDatoForHelePerioden = mapSluttdato(fakturaserieDto.perioder)
        return Fakturaserie(
            id = null,
            referanse = fakturaserieDto.fakturaserieReferanse,
            fakturaGjelderInnbetalingstype = fakturaserieDto.fakturaGjelderInnbetalingstype,
            fodselsnummer = fakturaserieDto.fodselsnummer,
            fullmektig = mapFullmektig(fakturaserieDto.fullmektig),
            referanseBruker = fakturaserieDto.referanseBruker,
            referanseNAV = fakturaserieDto.referanseNAV,
            startdato = startDatoForHelePerioden,
            sluttdato = sluttDatoForHelePerioden,
            intervall = fakturaserieDto.intervall,
            faktura = fakturaGenerator.lagFakturaerFor(
                startDatoForHelePerioden,
                sluttDatoForHelePerioden,
                fakturaserieDto.perioder,
                fakturaserieDto.intervall
            ),
        )
    }

    private fun mapFullmektig(fullmektigDto: Fullmektig?): Fullmektig? {
        if (fullmektigDto != null) {
            return Fullmektig(
                fodselsnummer = fullmektigDto.fodselsnummer,
                organisasjonsnummer = fullmektigDto.organisasjonsnummer,
                kontaktperson = fullmektigDto.kontaktperson
            )
        }
        return null
    }

    private fun mapStartdato(perioder: List<FakturaseriePeriode>): LocalDate {
        return perioder.minByOrNull { it.startDato }!!.startDato
    }

    private fun mapSluttdato(perioder: List<FakturaseriePeriode>): LocalDate {
        return perioder.maxByOrNull { it.sluttDato }!!.sluttDato
    }
}
