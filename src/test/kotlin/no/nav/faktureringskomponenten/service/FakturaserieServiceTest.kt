package no.nav.faktureringskomponenten.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class FakturaserieServiceTest {
    private val fakturaserieRepository = mockk<FakturaserieRepository>(relaxed = true)
    private val fakturaserieGenerator = mockk<FakturaserieGenerator>(relaxed = true)

    private val fakturaserieService = FakturaserieService(fakturaserieRepository, fakturaserieGenerator)

    @Test
    fun `Endrer fakturaserie, kansellerer opprinnelig og lager ny`() {
        val opprinneligReferanse = "MEL-123"
        val nyReferanse = "MEL-456"
        val opprinneligFakturaserie = lagFakturaserie(opprinneligReferanse)
        val nyFakturaserieDto = lagFakturaserieDto(nyReferanse)
        val nyFakturaserie = lagFakturaserie(nyReferanse)

        every {
            fakturaserieRepository.findByReferanse(opprinneligReferanse)
        } returns opprinneligFakturaserie

        every {
            fakturaserieRepository.findByReferanse(opprinneligReferanse)
        } returns opprinneligFakturaserie

        every {
            fakturaserieGenerator.lagFakturaserie(nyFakturaserieDto, any())
        } returns nyFakturaserie

        every {
            fakturaserieRepository.save(opprinneligFakturaserie)
        } returns opprinneligFakturaserie

        every {
            fakturaserieRepository.save(nyFakturaserie)
        } returns nyFakturaserie


        fakturaserieService.endreFakturaserie(opprinneligReferanse, nyFakturaserieDto)

        val oppdatertOpprinneligFakturaserie =
            fakturaserieRepository.findByReferanse(referanse = opprinneligReferanse)

        oppdatertOpprinneligFakturaserie?.status
            .shouldBe(FakturaserieStatus.ERSTATTET)

        verify {
            fakturaserieRepository.findByReferanse(opprinneligReferanse)
            fakturaserieRepository.save(opprinneligFakturaserie)
            fakturaserieRepository.save(nyFakturaserie)
        }
    }

    fun lagFakturaserie(vedtaksId: String): Fakturaserie {
        return Fakturaserie(
            id = 100,
            referanse = vedtaksId,
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
        referanse: String = UUID.randomUUID().toString(),
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
            referanse,
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
