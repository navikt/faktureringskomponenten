package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.faktureringskomponenten.config.ToggleName
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.avregning.AvregningBehandler
import no.nav.faktureringskomponenten.service.avregning.AvregningsfakturaGenerator
import org.junit.jupiter.api.Test
import ulid.ULID
import java.math.BigDecimal
import java.time.LocalDate

class KanselleringServiceTest {

    private val fakturaserieRepository = mockk<FakturaserieRepository>()
    private val unleash: FakeUnleash = FakeUnleash().apply { enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER) }
    private val fakturaserieGenerator =
        FakturaserieGenerator(FakturaGenerator(FakturaLinjeGenerator(), FakeUnleash(), 0), AvregningBehandler(AvregningsfakturaGenerator()), unleash)
    private val fakturaBestillingService = mockk<FakturaBestillingService>()

    private val kanselleringService = KanselleringService(
        fakturaserieRepository,
        fakturaserieGenerator,
        fakturaBestillingService,
        unleash
    )

    @Test
    fun `Kansellere fakturaserie som går tilbake et år`() {
        val fom = LocalDate.now().withMonth(7).withDayOfMonth(1)
        val tom = LocalDate.now().withMonth(12).withDayOfMonth(31)

        val aktivFakturaserie = Fakturaserie.forTest {
            startdato = fom
            sluttdato = tom
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom.withMonth(7).minusYears(1)
                    periodeTil = tom.withMonth(9).minusYears(1)
                    månedspris = 5000
                }
            }
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom.withMonth(10).minusYears(1)
                    periodeTil = tom.minusYears(1)
                    månedspris = 6000
                }
            }
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom
                    periodeTil = tom.withMonth(3)
                    månedspris = 10000
                }
            }
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom.withMonth(4)
                    periodeTil = tom.withMonth(6)
                    månedspris = 10000
                }
            }
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom.withMonth(7)
                    periodeTil = tom.withMonth(9)
                    månedspris = 10000
                }
            }
            faktura {
                status = FakturaStatus.OPPRETTET
                fakturaLinje {
                    periodeFra = fom.withMonth(10)
                    periodeTil = tom
                    månedspris = 10000
                }
            }
        }


        every { fakturaserieRepository.findByReferanse(aktivFakturaserie.referanse) } returns aktivFakturaserie
        every { fakturaserieRepository.findAllByReferanse(aktivFakturaserie.referanse) } returns listOf(
            aktivFakturaserie
        )

        val krediteringFakturaserie = mutableListOf<Fakturaserie>()
        every { fakturaserieRepository.save(aktivFakturaserie) } returns aktivFakturaserie
        every { fakturaserieRepository.save(not(aktivFakturaserie)) } answers {
            val fakturaserie = firstArg<Fakturaserie>()
            krediteringFakturaserie.add(fakturaserie)
            fakturaserie
        }
        justRun { fakturaBestillingService.bestillKreditnota(any()) }


        kanselleringService.kansellerFakturaserie(aktivFakturaserie.referanse, emptyList())


        verify { fakturaserieRepository.save(aktivFakturaserie) }
        verify { fakturaBestillingService.bestillKreditnota(krediteringFakturaserie.single()) }

        krediteringFakturaserie.single()
            .also { it.startdato shouldBe LocalDate.now().withDayOfMonth(1).withMonth(7).minusYears(1) }
            .also { it.sluttdato shouldBe LocalDate.now().withMonth(9).withDayOfMonth(30) }

        val faktura = krediteringFakturaserie.single().faktura
        faktura.shouldHaveSize(2)
        faktura.sortedBy { it.getPeriodeFra() }
            .run {
                get(0).fakturaLinje.single().belop shouldBe BigDecimal(-11000).setScale(2)
//                get(0).krediteringFakturaRef shouldBe aktivFakturaserie.faktura.first().referanseNr
                get(1).fakturaLinje.single().belop shouldBe BigDecimal(-30000).setScale(2)
            }


        aktivFakturaserie.run {
            status.shouldBe(FakturaserieStatus.KANSELLERT)
        }
    }

    @Test
    fun `kansellere faktura som går over flere år tilbake og har årsavregning`() {
        val fom = LocalDate.now().withMonth(7).withDayOfMonth(1)
        val tom = LocalDate.now().withMonth(12).withDayOfMonth(31)

        val aktivFakturaserie = Fakturaserie.forTest {
            referanse = ULID.randomULID()
            startdato = fom
            sluttdato = tom
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom.withMonth(7).minusYears(1)
                    periodeTil = tom.withMonth(9).minusYears(1)
                    månedspris = 5000
                }
            }
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom.withMonth(10).minusYears(1)
                    periodeTil = tom.minusYears(1)
                    månedspris = 6000
                }
            }
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom
                    periodeTil = tom.withMonth(3)
                    månedspris = 10000
                }
            }
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom.withMonth(4)
                    periodeTil = tom.withMonth(6)
                    månedspris = 10000
                }
            }
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom.withMonth(7)
                    periodeTil = tom.withMonth(9)
                    månedspris = 10000
                }
            }
            faktura {
                status = FakturaStatus.OPPRETTET
                fakturaLinje {
                    periodeFra = fom.withMonth(10)
                    periodeTil = tom
                    månedspris = 10000
                }
            }
        }

        val årsavregningForrigeÅr = Fakturaserie.forTest {
            referanse = ULID.randomULID()
            startdato = fom.minusYears(1)
            sluttdato = tom.minusYears(1)
            fakturaGjelderInnbetalingstype = Innbetalingstype.AARSAVREGNING
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom.minusYears(1)
                    periodeTil = tom.minusYears(1)
                    månedspris = -500
                }
            }
        }
        val årsavregningForrigeÅrNyBehandling = Fakturaserie.forTest {
            referanse = ULID.randomULID()
            startdato = fom.minusYears(1)
            sluttdato = tom.minusYears(1)
            fakturaGjelderInnbetalingstype = Innbetalingstype.AARSAVREGNING
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom.minusYears(1)
                    periodeTil = tom.minusYears(1)
                    månedspris = 300
                }
            }
        }


        every { fakturaserieRepository.findByReferanse(aktivFakturaserie.referanse) } returns aktivFakturaserie
        every { fakturaserieRepository.findByReferanse(årsavregningForrigeÅr.referanse) } returns årsavregningForrigeÅr
        every { fakturaserieRepository.findByReferanse(årsavregningForrigeÅrNyBehandling.referanse) } returns årsavregningForrigeÅrNyBehandling
        every { fakturaserieRepository.findAllByReferanse(aktivFakturaserie.referanse) } returns listOf(
            aktivFakturaserie
        )

        every { fakturaserieRepository.save(any()) } answers { firstArg() }

        val krediteringFakturaserie = mutableListOf<Fakturaserie>()
        every { fakturaBestillingService.bestillKreditnota(capture(krediteringFakturaserie)) } just Runs


        kanselleringService.kansellerFakturaserie(
            aktivFakturaserie.referanse,
            listOf(årsavregningForrigeÅr.referanse, årsavregningForrigeÅrNyBehandling.referanse)
        )


        verify { fakturaserieRepository.save(aktivFakturaserie) }
        verify { fakturaserieRepository.save(årsavregningForrigeÅr) }
        verify { fakturaserieRepository.save(årsavregningForrigeÅrNyBehandling) }
        verify { fakturaserieRepository.save(krediteringFakturaserie.single()) }
        verify { fakturaBestillingService.bestillKreditnota(krediteringFakturaserie.single()) }

        krediteringFakturaserie.single()
            .also { it.startdato shouldBe LocalDate.now().withDayOfMonth(1).withMonth(7).minusYears(1) }
            .also { it.sluttdato shouldBe LocalDate.now().withMonth(9).withDayOfMonth(30) }

        val faktura = krediteringFakturaserie.single().faktura
        faktura.shouldHaveSize(2)
        faktura.sortedBy { it.getPeriodeFra() }
            .run {
                get(0).fakturaLinje.single().belop shouldBe BigDecimal(-10800).setScale(2)
//                get(0).krediteringFakturaRef shouldBe aktivFakturaserie.faktura.first().referanseNr
                get(1).fakturaLinje.single().belop shouldBe BigDecimal(-30000).setScale(2)
            }


        aktivFakturaserie.run {
            status.shouldBe(FakturaserieStatus.KANSELLERT)
        }
        årsavregningForrigeÅr.run {
            status.shouldBe(FakturaserieStatus.KANSELLERT)
        }
        årsavregningForrigeÅrNyBehandling.run {
            status.shouldBe(FakturaserieStatus.KANSELLERT)
        }
    }
}
