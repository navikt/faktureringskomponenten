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
import java.util.UUID

class FakturaserieServiceTest {
    private val fakturaserieRepository = mockk<FakturaserieRepository>(relaxed = true)
    private val fakturaserieMapper = mockk<FakturaserieMapper>(relaxed = true)

    private val fakturaserieService = FakturaserieService(fakturaserieRepository, fakturaserieMapper)

    @Test
    fun `Endrer fakturaserie, kansellerer opprinnelig og lager ny`() {
        val opprinneligReferanseId = "MEL-123"
        val nyReferanseId = "MEL-456"
        val opprinneligFakturaserie = lagFakturaserie(opprinneligReferanseId)
        val nyFakturaserieDto = lagFakturaserieDto(nyReferanseId)
        val nyFakturaserie = lagFakturaserie(nyReferanseId)

        every {
            fakturaserieRepository.findFakturaserieByReferanseIdAndStatusIn(opprinneligReferanseId)
        } returns opprinneligFakturaserie

        every {
            fakturaserieRepository.findByReferanseId(opprinneligReferanseId)
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


        fakturaserieService.endreFakturaserie(opprinneligReferanseId, nyFakturaserieDto)

        val oppdatertOpprinneligFakturaserie =
            fakturaserieRepository.findByReferanseId(referanseId = opprinneligReferanseId)

        oppdatertOpprinneligFakturaserie?.status
            .shouldBe(FakturaserieStatus.ERSTATTET)

        verify(exactly = 1) {
            fakturaserieRepository.findByReferanseId(opprinneligReferanseId)
            fakturaserieRepository.findFakturaserieByReferanseIdAndStatusIn(opprinneligReferanseId)
            fakturaserieRepository.save(opprinneligFakturaserie)
            fakturaserieRepository.save(nyFakturaserie)
        }
    }

    fun lagFakturaserie(vedtaksId: String): Fakturaserie {
        return Fakturaserie(
            id = 100,
            referanseId = vedtaksId,
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
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
        referanseId: String = UUID.randomUUID().toString(),
        fodselsnummer: String = "12345678911",
        fullmektig: Fullmektig = Fullmektig("11987654321", "123456789", "Ole Brum"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "NAV referanse",
        fakturaGjelderInnbetalingstype: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
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
            referanseId,
            fodselsnummer,
            fullmektig,
            referanseBruker,
            referanseNav,
            fakturaGjelderInnbetalingstype,
            intervall,
            fakturaseriePeriode
        )
    }
}
