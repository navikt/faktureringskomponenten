package no.nav.faktureringskomponenten.service

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.faktureringskomponenten.config.ToggleName
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.models.forTest
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.avregning.AvregningBehandler
import no.nav.faktureringskomponenten.service.avregning.AvregningsfakturaGenerator
import org.junit.jupiter.api.Test
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
    fun `Kansellere fakturaserie som kun er inneværende år og fremover`() {
        val fom = LocalDate.now().withMonth(1).withDayOfMonth(1)
        val tom = LocalDate.now().withMonth(12).withDayOfMonth(31)

        val eksisterendeFakturaserie = Fakturaserie.forTest {
            startdato = fom
            sluttdato = tom
            faktura {
                status = FakturaStatus.BESTILT
                fakturaLinje {
                    periodeFra = fom
                    periodeTil = tom
                    månedspris = 10000
                }
            }
        }
        val tidligereFakturaserie = Fakturaserie.forTest {
            startdato = fom.minusYears(1)
            sluttdato = tom.minusYears(1)
        }


        every { fakturaserieRepository.findByReferanse(eksisterendeFakturaserie.referanse) } returns eksisterendeFakturaserie
        every { fakturaserieRepository.findAllByReferanse(eksisterendeFakturaserie.referanse) } returns listOf(
            eksisterendeFakturaserie, tidligereFakturaserie
        )

        val krediteringFakturaserie = mutableListOf<Fakturaserie>()
        every { fakturaserieRepository.save(eksisterendeFakturaserie) } returns eksisterendeFakturaserie
        every { fakturaserieRepository.save(not(eksisterendeFakturaserie)) } answers {
            val fakturaserie = firstArg<Fakturaserie>()
            krediteringFakturaserie.add(fakturaserie)
            fakturaserie
        }
        justRun { fakturaBestillingService.bestillKreditnota(any()) }


        kanselleringService.kansellerFakturaserie(eksisterendeFakturaserie.referanse)


        verify { fakturaserieRepository.save(eksisterendeFakturaserie) }
        verify { fakturaBestillingService.bestillKreditnota(krediteringFakturaserie.single()) }

        krediteringFakturaserie.single()
            .also { it.startdato shouldBe fom }
            .also { it.sluttdato shouldBe tom }
            .faktura.single()
            .also { it.krediteringFakturaRef shouldBe eksisterendeFakturaserie.faktura.single().referanseNr }
            .fakturaLinje.single()
            .also { it.belop shouldBe BigDecimal(-10000).setScale(2) }


        eksisterendeFakturaserie.run {
            status.shouldBe(FakturaserieStatus.KANSELLERT)
            faktura.shouldHaveSize(1)
                .first()
                .status.shouldBe(FakturaStatus.BESTILT)
        }
    }


}
