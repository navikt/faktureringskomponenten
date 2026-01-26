package no.nav.faktureringskomponenten.service

import io.getunleash.Unleash
import no.nav.faktureringskomponenten.config.ToggleName
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ulid.ULID
import java.math.BigDecimal
import java.time.LocalDate

@Service
class KanselleringService(
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaserieGenerator: FakturaserieGenerator,
    private val fakturaBestillingService: FakturaBestillingService,
    private val unleash: Unleash
) {

    @Transactional
    fun kansellerFakturaserie(referanse: String): String {
        val eksisterendeFakturaserie = fakturaserieRepository.findByReferanse(referanse)
            ?: throw RessursIkkeFunnetException(
                field = "fakturaserieId",
                message = "Finner ikke fakturaserie med referanse $referanse"
            )

        val alleFakturaserier = hentFakturaserier(eksisterendeFakturaserie.referanse)
        val tidligsteFakturaserieStartDato = alleFakturaserier.minBy { it.startdato }.startdato
        val startDato = if (unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
            maxOf(tidligsteFakturaserieStartDato, LocalDate.of(LocalDate.now().year, 1, 1))
        } else tidligsteFakturaserieStartDato
        val sluttDato = alleFakturaserier.maxBy { it.startdato }.sluttdato

        val fakturaserieDto = FakturaserieDto(
            fakturaserieReferanse = ULID.randomULID(),
            fodselsnummer = eksisterendeFakturaserie.fodselsnummer,
            fullmektig = eksisterendeFakturaserie.fullmektig,
            referanseBruker = eksisterendeFakturaserie.referanseBruker,
            referanseNAV = eksisterendeFakturaserie.referanseNAV,
            fakturaGjelderInnbetalingstype = eksisterendeFakturaserie.fakturaGjelderInnbetalingstype,
            intervall = eksisterendeFakturaserie.intervall,
            perioder = listOf(FakturaseriePeriode(BigDecimal.ZERO, startDato, sluttDato, "Kansellering"))
        )

        val bestilteFakturaer = if (unleash.isEnabled(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)) {
            eksisterendeFakturaserie.bestilteFakturaer().filter { it.alleFakturaLinjerErFraIÅrEllerFremover() }
        } else {
            eksisterendeFakturaserie.bestilteFakturaer()
        }

        val krediteringFakturaserie = fakturaserieGenerator.lagFakturaserieForKansellering(
            fakturaserieDto,
            startDato,
            sluttDato,
            bestilteFakturaer
        )

        fakturaserieRepository.save(krediteringFakturaserie)

        eksisterendeFakturaserie.kansellerMed(krediteringFakturaserie)
        fakturaserieRepository.save(eksisterendeFakturaserie)

        fakturaBestillingService.bestillKreditnota(krediteringFakturaserie)
        return krediteringFakturaserie.referanse
    }

    fun hentFakturaserier(referanse: String): List<Fakturaserie> {
        return fakturaserieRepository.findAllByReferanse(referanse)
    }
}
