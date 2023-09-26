package no.nav.faktureringskomponenten.service

import io.kotest.matchers.collections.shouldContain
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
    private val fakturaserieRepository = mockk<FakturaserieRepository>()

    private val fakturaserieService = FakturaserieService(fakturaserieRepository, FakturaserieGenerator())

    @Test
    fun `Endrer fakturaserie, erstatter opprinnelig og lager ny`() {
        val opprinneligFakturaserie = lagFakturaserie()
        val nyFakturaserieDto = lagFakturaserieDto("MEL-456")

        every {
            fakturaserieRepository.findByReferanse("MEL-123")
        } returns opprinneligFakturaserie

        every { fakturaserieRepository.save(any()) } returns mockk()


        fakturaserieService.endreFakturaserie("MEL-123", nyFakturaserieDto)


        val fakturaserier = mutableListOf<Fakturaserie>()
        verify {
            fakturaserieRepository.save(capture(fakturaserier))
        }
        fakturaserier shouldContain opprinneligFakturaserie
        opprinneligFakturaserie.status shouldBe  FakturaserieStatus.ERSTATTET
        fakturaserier.size shouldBe 2
    }

    private fun lagFakturaserie(): Fakturaserie {
        return Fakturaserie(
            id = 100,
            referanse = "MEL-123",
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

    private fun lagFakturaserieDto(
        referanse: String = UUID.randomUUID().toString(),
        fodselsnummer: String = "12345678911",
        fullmektig: Fullmektig = Fullmektig("11987654321", "123456789", "Ole Brum"),
        referanseBruker: String = "Nasse Nøff",
        referanseNav: String = "Referanse NAV",
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
