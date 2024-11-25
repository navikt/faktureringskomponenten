package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.inspectors.forExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.lagFaktura
import no.nav.faktureringskomponenten.lagFakturaserie
import no.nav.faktureringskomponenten.service.avregning.AvregningBehandler
import no.nav.faktureringskomponenten.service.avregning.AvregningsfakturaGenerator
import org.junit.jupiter.api.Test
import ulid.ULID
import java.math.BigDecimal
import java.time.LocalDate

private const val OPPRINNELIG_REF = "123"
private const val NY_REF = "456"

class FakturaserieServiceTest {
    private val fakturaserieRepository = mockk<FakturaserieRepository>()
    private val fakturaserieGenerator =
        FakturaserieGenerator(FakturaGenerator(FakturaLinjeGenerator(), FakeUnleash(), 0))
    private val avregningBehandler = AvregningBehandler(AvregningsfakturaGenerator())
    private val fakturaBestillingService = mockk<FakturaBestillingService>()
    private val fakturaGenerator = mockk<FakturaGenerator>(relaxed = true)

    private val fakturaserieService =
        FakturaserieService(
            fakturaserieRepository,
            fakturaserieGenerator,
            avregningBehandler,
            fakturaBestillingService,
            fakturaGenerator
        )

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

        opprinneligFakturaserie.status shouldBe FakturaserieStatus.ERSTATTET
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
            it.periodeFra shouldBe LocalDate.of(2024, 1, 1)
            it.periodeTil shouldBe LocalDate.of(2024, 3, 31)
            it.antall shouldBe BigDecimal(1)
            it.enhetsprisPerManed shouldBe BigDecimal("1000.00")
            it.belop shouldBe BigDecimal("1000.00")
        }
    }

    @Test
    fun `Endrer faktura-mottaker, finnes ikke gjenstående fakturaer, gjør ingenting`() {
        every { fakturaserieRepository.findByReferanse(OPPRINNELIG_REF) } returns Fakturaserie(
            faktura = mutableListOf(Faktura().apply { status = FakturaStatus.BESTILT })
        )


        fakturaserieService.endreFakturaMottaker(OPPRINNELIG_REF, FakturamottakerDto(Fullmektig()))


        verify { fakturaserieRepository.save(any()) wasNot Called }
    }

    @Test
    fun `Endrer faktura-mottaker, mottaker har ikke endret seg, gjør ingenting`() {
        every { fakturaserieRepository.findByReferanse(OPPRINNELIG_REF) } returns Fakturaserie(
            faktura = mutableListOf(Faktura().apply { status = FakturaStatus.OPPRETTET }),
            fullmektig = Fullmektig("123", null)
        )


        fakturaserieService.endreFakturaMottaker(OPPRINNELIG_REF, FakturamottakerDto(Fullmektig("123", null)))


        verify { fakturaserieRepository.save(any()) wasNot Called }
    }

    @Test
    fun `Endrer faktura-mottaker, motter er ny, endrer fullmektig i fakturaserie`() {
        val fakturaserie = Fakturaserie(
            faktura = mutableListOf(Faktura().apply { status = FakturaStatus.OPPRETTET }),
            fullmektig = Fullmektig("123", null)
        )
        val lagretFakturaserie = mutableListOf<Fakturaserie>()
        every { fakturaserieRepository.findByReferanse(OPPRINNELIG_REF) } returns fakturaserie
        every { fakturaserieRepository.save(capture(lagretFakturaserie)) } returns mockk()


        fakturaserieService.endreFakturaMottaker(OPPRINNELIG_REF, FakturamottakerDto(null))


        verify { fakturaserieRepository.save(any()) }
        fakturaserie.fullmektig.shouldBeNull()
    }

    @Test
    fun `Kansellere fakturaserie - eksisterende får oppdatert fakturaseriestatus til kansellert og nye faktura bestilles`() {
        val eksisterendeFakturaserie = lagFakturaserie {
            faktura(
                lagFaktura {
                    status(FakturaStatus.BESTILT)
                }
            )
        }
        val tidligereFakturaserie = lagFakturaserie { }


        every { fakturaserieRepository.findAllByReferanse(eksisterendeFakturaserie.referanse) } returns listOf(
            tidligereFakturaserie
        )
        every { fakturaserieRepository.findByReferanse(eksisterendeFakturaserie.referanse) } returns eksisterendeFakturaserie

        val fakturaserieCapture = mutableListOf<Fakturaserie>()
        every { fakturaserieRepository.save(capture(fakturaserieCapture)) } returns mockk()
        every { fakturaserieRepository.save(eksisterendeFakturaserie) } returns eksisterendeFakturaserie
        justRun { fakturaBestillingService.bestillKreditnota(any()) }


        fakturaserieService.kansellerFakturaserie(eksisterendeFakturaserie.referanse)


        verify { fakturaserieRepository.save(eksisterendeFakturaserie) }
        verify { fakturaBestillingService.bestillKreditnota(fakturaserieCapture.single()) }

        fakturaserieCapture.single()
            .faktura.single()
            .run {
                krediteringFakturaRef.isNotEmpty()
                fakturaLinje.single()
                    .belop.shouldBe(BigDecimal(-10000).setScale(2))
            }


        eksisterendeFakturaserie.run {
            status.shouldBe(FakturaserieStatus.KANSELLERT)
            faktura.shouldHaveSize(1)
                .first()
                .status.shouldBe(FakturaStatus.BESTILT)
        }
    }

    @Test
    fun `lag ny faktura`() {
        val fakturaDto = FakturaDto(
            ULID.randomULID(),
            ULID.randomULID(),
            "123456789",
            Fullmektig("11987654321", "123456789"),
            "Nasse Nøff",
            "Referanse NAV",
            Innbetalingstype.TRYGDEAVGIFT,
            BigDecimal.valueOf(2500),
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
            "Testfaktura"
        )
        val fakturaSerieSlot = slot<Fakturaserie>()
        val opprinneligFakturaserie = lagOpprinneligFakturaserie()

        every { fakturaserieRepository.findByReferanse(fakturaDto.tidligereFakturaserieReferanse) } returns opprinneligFakturaserie
        every { fakturaserieRepository.save(capture(fakturaSerieSlot)) } returns Fakturaserie()


        val nyFakturaSerieReferanse = fakturaserieService.lagNyFaktura(fakturaDto)


        fakturaSerieSlot.captured.run {
            referanse.shouldBe(nyFakturaSerieReferanse)
            intervall.shouldBe(FakturaserieIntervall.SINGEL)
            faktura.single().run {
                krediteringFakturaRef.shouldBe("1234")
                fakturaLinje.single().run {
                    belop.shouldBe(BigDecimal.valueOf(2500))
                    periodeFra.shouldBe(LocalDate.of(2024, 1, 1))
                    periodeTil.shouldBe(LocalDate.of(2024, 12, 31))
                    beskrivelse.shouldBe("Testfaktura")
                    antall.shouldBe(BigDecimal.ONE)
                    enhetsprisPerManed.shouldBe(BigDecimal.ZERO)
                }
            }
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
            faktura = mutableListOf(
                Faktura(
                    id = 1,
                    referanseNr = "1234",
                    datoBestilt = LocalDate.of(2023, 12, 19),
                    status = FakturaStatus.BESTILT,
                    eksternFakturaNummer = "8272123",
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
                    referanseNr = "5678",
                    datoBestilt = LocalDate.of(2024, 3, 19),
                    status = FakturaStatus.BESTILT,
                    eksternFakturaNummer = "8272123",
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
                organisasjonsnummer = ""
            ),
        )
    }

    private fun lagFakturaserieDto(
        referanse: String = ULID.randomULID(),
        fodselsnummer: String = "12345678911",
        fullmektig: Fullmektig = Fullmektig("11987654321", "123456789"),
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
                LocalDate.of(2024, 2, 29),
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
