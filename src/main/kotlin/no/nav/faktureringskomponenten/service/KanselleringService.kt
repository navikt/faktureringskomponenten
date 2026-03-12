package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ulid.ULID
import java.math.BigDecimal

@Service
class KanselleringService(
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaserieGenerator: FakturaserieGenerator,
    private val fakturaBestillingService: FakturaBestillingService,
) {

    @Transactional
    fun kansellerFakturaserie(referanse: String, årsavregningRef: List<String>): String {
        val aktivFakturaserie = fakturaserieRepository.findByReferanse(referanse)
            ?: throw RessursIkkeFunnetException(
                field = "fakturaserieId",
                message = "Finner ikke fakturaserie med referanse $referanse"
            )

        check(aktivFakturaserie.erAktiv()) {
            "Kan ikke kansellere fakturaserie med status ${aktivFakturaserie.status}"
        }

        val alleÅrsavregningFakturaserier = årsavregningRef.map { ref ->
            val fakturaserie = fakturaserieRepository.findByReferanse(ref)
                ?: throw RessursIkkeFunnetException(
                    field = "årsavregningRef",
                    message = "Finner ikke årsavregning-fakturaserie med referanse $ref"
                )
            require(fakturaserie.fakturaGjelderInnbetalingstype == Innbetalingstype.AARSAVREGNING) {
                "Fakturaserie med referanse $ref er ikke en årsavregning, men ${fakturaserie.fakturaGjelderInnbetalingstype}"
            }
            fakturaserie
        }
        val alleFakturaserier = hentFakturaserier(aktivFakturaserie.referanse)
        val alleBestilteFakturalinjer = (alleFakturaserier + alleÅrsavregningFakturaserier)
            .flatMap { it.faktura }
            .filter { it.status == FakturaStatus.BESTILT }
            .flatMap { it.fakturaLinje }
            .groupBy { it.periodeFra.year }

        if (alleBestilteFakturalinjer.isEmpty()) {
            aktivFakturaserie.kanseller()
            fakturaserieRepository.save(aktivFakturaserie)
            alleÅrsavregningFakturaserier.forEach {
                it.kanseller()
                fakturaserieRepository.save(it)
            }
            return "Kansellert"
        }

        val fakturalinjer = alleBestilteFakturalinjer.values.flatten()
        val startDato = fakturalinjer.minOfOrNull { it.periodeFra } ?: alleFakturaserier.minOf { it.startdato }
        val sluttDato = fakturalinjer.maxOfOrNull { it.periodeTil } ?: alleFakturaserier.maxOf { it.sluttdato }

        val fakturaserieDto = FakturaserieDto(
            fakturaserieReferanse = ULID.randomULID(),
            fodselsnummer = aktivFakturaserie.fodselsnummer,
            fullmektig = aktivFakturaserie.fullmektig,
            referanseBruker = aktivFakturaserie.referanseBruker,
            referanseNAV = aktivFakturaserie.referanseNAV,
            fakturaGjelderInnbetalingstype = aktivFakturaserie.fakturaGjelderInnbetalingstype,
            intervall = aktivFakturaserie.intervall,
            perioder = listOf(FakturaseriePeriode(BigDecimal.ZERO, startDato, sluttDato, "Kansellering"))
        )

        val krediteringFakturaserie = fakturaserieGenerator.lagFakturaserieForKansellering(
            fakturaserieDto,
            startDato,
            sluttDato,
            alleBestilteFakturalinjer
        )

        fakturaserieRepository.save(krediteringFakturaserie)

        aktivFakturaserie.kansellerMed(krediteringFakturaserie)
            .also { fakturaserieRepository.save(aktivFakturaserie) }

        alleÅrsavregningFakturaserier.forEach {
            it.kansellerMed(krediteringFakturaserie)
            fakturaserieRepository.save(it)
        }

        fakturaBestillingService.bestillKreditnota(krediteringFakturaserie)
        return krediteringFakturaserie.referanse
    }

    fun hentFakturaserier(referanse: String): List<Fakturaserie> {
        return fakturaserieRepository.findAllByReferanse(referanse)
    }
}
