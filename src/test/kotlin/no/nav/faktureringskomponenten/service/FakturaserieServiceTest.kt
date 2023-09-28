package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.inspectors.forExactly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

private const val OPPRINNELIG_REF = "123"
private const val NY_REF = "456"

class FakturaserieServiceTest {
    private val fakturaserieRepository = mockk<FakturaserieRepository>()
    private val fakturaserieGenerator = FakturaserieGenerator(FakturaGenerator(FakturaLinjeGenerator(), FakeUnleash()))

    private val fakturaserieService = FakturaserieService(fakturaserieRepository, fakturaserieGenerator)

    @Test
    fun `Endrer fakturaserie, erstatter opprinnelig og lager ny`() {
        val opprinneligFakturaserie = lagOpprinneligFakturaserie()
        every {
            fakturaserieRepository.findByReferanse(OPPRINNELIG_REF)
        } returns opprinneligFakturaserie

        val fakturaserier = mutableListOf<Fakturaserie>()
        every { fakturaserieRepository.save(capture(fakturaserier)) } returns mockk()

        val nyFakturaserieDto = lagFakturaserieDto(NY_REF)


        fakturaserieService.erstattFakturaserie(OPPRINNELIG_REF, nyFakturaserieDto)


        val nyFakturaserie = fakturaserier.single { it.referanse == NY_REF }

        opprinneligFakturaserie.status shouldBe  FakturaserieStatus.ERSTATTET
        opprinneligFakturaserie.erstattetMed shouldNotBe null
        opprinneligFakturaserie.erstattetMed!!.referanse shouldBe nyFakturaserie.referanse

        nyFakturaserie.status shouldBe FakturaserieStatus.OPPRETTET
    }

    @Test
    fun `Endrer fakturaserie, fakturaer har blitt sendt for 2 kvartaler, avregning`() {
        val opprinneligFakturaserie = lagOpprinneligFakturaserie()
        every {
            fakturaserieRepository.findByReferanse(OPPRINNELIG_REF)
        } returns opprinneligFakturaserie

        val fakturaserier = mutableListOf<Fakturaserie>()
        every { fakturaserieRepository.save(capture(fakturaserier)) } returns mockk()

        val nyFakturaserieDto = lagFakturaserieDto(NY_REF)


        fakturaserieService.erstattFakturaserie(OPPRINNELIG_REF, nyFakturaserieDto)


        val nyFakturaserie = fakturaserier.filter { it.referanse == NY_REF }.single()
        val fakturaLinjer = nyFakturaserie.faktura.flatMap { it.fakturaLinje }
        fakturaLinjer.forExactly(1) {
            it.referertFakturaVedAvregning shouldNotBe null
            it.periodeFra shouldBe LocalDate.of(2024, 1, 1)
            it.periodeTil shouldBe LocalDate.of(2024, 3, 31)
            it.antall shouldBe 1
            it.enhetsprisPerManed shouldBe 1000
            it.belop shouldBe 1000
        }
    }

    private fun lagOpprinneligFakturaserie(): Fakturaserie {
        return Fakturaserie(
            id = 100,
            referanse = OPPRINNELIG_REF,
            fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
            referanseBruker = "Referanse bruker",
            referanseNAV = "Referanse NAV",
            startdato = LocalDate.of(2024, 1, 1),
            sluttdato = LocalDate.of(2024, 12, 31),
            status = FakturaserieStatus.OPPRETTET,
            intervall = FakturaserieIntervall.KVARTAL,
            faktura = listOf(
                Faktura(
                    id = 1,
                    datoBestilt = LocalDate.of(2023, 12, 19),
                    status = FakturaStatus.BESTILLT,
                    fakturaLinje = listOf(
                                FakturaLinje(
                                    id = 1,
                                    periodeFra = LocalDate.of(2024, 1, 1),
                                    periodeTil = LocalDate.of(2024, 3, 31),
                                    beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                                    antall = BigDecimal(3),
                                    enhetsprisPerManed = BigDecimal(1000),
                                    belop = BigDecimal(3000),
                                ),
                                FakturaLinje(
                                    id = 2,
                                    periodeFra = LocalDate.of(2024, 1, 1),
                                    periodeTil = LocalDate.of(2024, 3, 31),
                                    beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                                    antall = BigDecimal(3),
                                    enhetsprisPerManed = BigDecimal(2000),
                                    belop = BigDecimal(6000),
                                ),
                            )
                ),
                Faktura(
                    id = 2,
                    datoBestilt = LocalDate.of(2024, 3, 19),
                    status = FakturaStatus.BESTILLT,
                    fakturaLinje = listOf(
                        FakturaLinje(
                            id = 3,
                            periodeFra = LocalDate.of(2024, 4, 1),
                            periodeTil = LocalDate.of(2024, 6, 30),
                            beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                            antall = BigDecimal(3),
                            enhetsprisPerManed = BigDecimal(1000),
                            belop = BigDecimal(6000),
                        ),
                        FakturaLinje(
                            id = 4,
                            periodeFra = LocalDate.of(2024, 4, 1),
                            periodeTil = LocalDate.of(2024, 6, 30),
                            beskrivelse = "Inntekt: X, Dekning: Y, Sats: Z",
                            antall = BigDecimal(3),
                            enhetsprisPerManed = BigDecimal(2000),
                            belop = BigDecimal(6000),
                        ),
                    )
                )
            ),
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
                BigDecimal.valueOf(1000),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                "Dekning: Pensjon og helsedel, Sats 10%"
            ),
            FakturaseriePeriode(
                BigDecimal.valueOf(2000),
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 28),
                "Dekning: Pensjon og helsedel, Sats 10%"
            ),
            FakturaseriePeriode(
                BigDecimal.valueOf(3000),
                LocalDate.of(2024, 3, 1),
                LocalDate.of(2024, 12, 31),
                "Dekning: Pensjon og helsedel, Sats 10%"
            ),
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
