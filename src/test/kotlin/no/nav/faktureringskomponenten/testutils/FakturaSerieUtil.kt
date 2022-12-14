package no.nav.faktureringskomponenten.testutils

import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieIntervallDto
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.FullmektigDto
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.models.Fullmektig
import org.assertj.core.internal.bytebuddy.utility.RandomString
import java.math.BigDecimal
import java.time.LocalDate

class FakturaSerieUtil {
    companion object {
        fun lagFakturaserieDto(
            vedtaksnummer: String = "VEDTAK-1" + RandomString.make(3),
            fodselsnummer: String = "12345678911",
            fullmektig: FullmektigDto = FullmektigDto("11987654321", "123456789", "Ole Brum"),
            referanseBruker: String = "Nasse Nøff",
            referanseNav: String = "NAV referanse",
            fakturaGjelder: String = "Trygdeavgift",
            intervall: FakturaserieIntervallDto = FakturaserieIntervallDto.KVARTAL,
            fakturaseriePeriode: List<FakturaseriePeriodeDto> = listOf(
                FakturaseriePeriodeDto(
                    BigDecimal.valueOf(123),
                    LocalDate.of(2022, 1, 1),
                    LocalDate.of(2022, 11, 30),
                    "Beskrivelse"
                )
            ),
        ): FakturaserieDto {
            return FakturaserieDto(
                vedtaksnummer,
                fodselsnummer,
                fullmektig,
                referanseBruker,
                referanseNav,
                fakturaGjelder,
                intervall,
                fakturaseriePeriode
            )
        }

        fun lagFakturaserie(): Fakturaserie {
            return Fakturaserie(
                100, vedtaksId = "MEL-1",
                fakturaGjelder = "FTRL",
                referanseBruker = "Referanse bruker",
                referanseNAV = "Referanse NAV",
                startdato = LocalDate.of(2022, 1, 1),
                sluttdato = LocalDate.of(2023, 5, 1),
                status = FakturaserieStatus.OPPRETTET,
                intervall = FakturaserieIntervall.KVARTAL,
                faktura = listOf(),
                fullmektig = Fullmektig(
                    fodselsnummer = BigDecimal(12129012345),
                    kontaktperson = "Test",
                    organisasjonsnummer = ""
                ),
                fodselsnummer = BigDecimal(12345678911)
            )
        }
    }
}