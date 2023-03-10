package no.nav.faktureringskomponenten.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.bytebuddy.utility.RandomString
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.mappers.FakturaserieMapper
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FakturaserieServiceTest {
    private val fakturaserieRepository = mockk<FakturaserieRepository>(relaxed = true)
    private val fakturaserieMapper = mockk<FakturaserieMapper>(relaxed = true)

    private val fakturaserieService = FakturaserieService(fakturaserieRepository, fakturaserieMapper)

    @Test
    fun `Endrer fakturaserie, kansellerer opprinnelig og lager ny`() {
        val opprinneligVedtaksId = "MEL-123"
        val nyVedtaksId = "MEL-456"
        val opprinneligFakturaserie = lagFakturaserie(opprinneligVedtaksId)
        val nyFakturaserieDto = lagFakturaserieDto(nyVedtaksId)
        val nyFakturaserie = lagFakturaserie(nyVedtaksId)

        every {
            fakturaserieRepository.findByVedtaksId(opprinneligVedtaksId)
        } returns opprinneligFakturaserie

        every {
            fakturaserieMapper.tilFakturaserie(nyFakturaserieDto, any())
        } returns nyFakturaserie

        every {
            fakturaserieRepository.save(opprinneligFakturaserie)
        } returns opprinneligFakturaserie

        every {
            fakturaserieRepository.save(nyFakturaserie)
        } returns nyFakturaserie

        fakturaserieService.endreFakturaserie(opprinneligVedtaksId, nyFakturaserieDto)

        val oppdatertOpprinneligFakturaserie =
            fakturaserieRepository.findByVedtaksId(vedtaksId = opprinneligVedtaksId)
        oppdatertOpprinneligFakturaserie?.status
            .shouldBe(FakturaserieStatus.KANSELLERT)

        verify(exactly = 2) {
            fakturaserieRepository.findByVedtaksId(opprinneligVedtaksId)
        }
        verify(exactly = 1) {
            fakturaserieRepository.save(opprinneligFakturaserie)
            fakturaserieRepository.save(nyFakturaserie)
        }
    }

    fun lagFakturaserie(vedtaksId: String): Fakturaserie {
        return Fakturaserie(
            id = 100,
            vedtaksId = vedtaksId,
            fakturaGjelder = "FTRL",
            referanseBruker = "Referanse bruker",
            referanseNAV = "Referanse NAV",
            startdato = LocalDate.of(2022, 1, 1),
            sluttdato = LocalDate.of(2023, 5, 1),
            status = FakturaserieStatus.OPPRETTET,
            intervall = FakturaserieIntervall.KVARTAL,
            faktura = listOf(),
            fodselsnummer = "12345678911",
            fullmektig = Fullmektig(
                fodselsnummer = "12129012345",
                kontaktperson = "Test",
                organisasjonsnummer = ""
            ),
        )
    }

    fun lagFakturaserieDto(
        vedtaksId: String = "VEDTAK-1" + RandomString.make(3),
        fodselsnummer: String = "12345678911",
        fullmektig: Fullmektig = Fullmektig("11987654321", "123456789", "Ole Brum"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "NAV referanse",
        fakturaGjelder: String = "Trygdeavgift",
        tema: FakturaserieTema = FakturaserieTema.TRY,
        intervall: FakturaserieIntervall = FakturaserieIntervall.KVARTAL,
        fakturaseriePeriode: List<FakturaseriePeriode> = listOf(
            FakturaseriePeriode(
                BigDecimal.valueOf(123),
                LocalDate.of(2022, 1, 1),
                LocalDate.of(2022, 11, 30),
                "Beskrivelse"
            )
        ),
    ): FakturaserieDto {
        return FakturaserieDto(
            vedtaksId,
            fodselsnummer,
            fullmektig,
            referanseBruker,
            referanseNav,
            fakturaGjelder,
            tema,
            intervall,
            fakturaseriePeriode
        )
    }
}
