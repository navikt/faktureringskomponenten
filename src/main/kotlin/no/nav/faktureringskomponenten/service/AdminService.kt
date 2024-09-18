package no.nav.faktureringskomponenten.service

import jakarta.transaction.Transactional
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.avregning.AvregningsfakturaGenerator
import org.springframework.stereotype.Service
import ulid.ULID
import java.time.LocalDate

@Service
class AdminService(
    val fakturaService: FakturaService,
    private val fakturaserieRepository: FakturaserieRepository,
) {
    @Transactional
    fun krediterFaktura(fakturaReferanse: String): Fakturaserie {
        val faktura = fakturaService.hentFaktura(fakturaReferanse) ?: throw RessursIkkeFunnetException(
            field = "fakturaReferanse",
            message = "Fant ikke faktura med referanse: $fakturaReferanse"
        )

        val fakturaserie = faktura.fakturaserie ?: throw RessursIkkeFunnetException(
            field = "fakturaserie",
            message = "Fant ikke fakturaserie for faktura med referanse: $fakturaReferanse"
        )

        if (fakturaserie.status != FakturaserieStatus.UNDER_BESTILLING) {
            throw IllegalStateException("Fakturaserie er ikke aktiv")
        }

        if (faktura.status != FakturaStatus.BESTILT) {
            throw IllegalStateException("Faktura er ikke i BESTILT status")
        }

        faktura.status = FakturaStatus.AVBRUTT

        val nyFaktura = Faktura(
            referanseNr = ULID.randomULID(),
            fakturaserie = fakturaserie,
            status = FakturaStatus.OPPRETTET,
            datoBestilt = LocalDate.now(),
            fakturaLinje = faktura.fakturaLinje.map {
                FakturaLinje(
                    periodeFra = it.periodeFra,
                    periodeTil = it.periodeTil,
                    belop = it.belop.negate(),
                    beskrivelse = AvregningsfakturaGenerator.genererBeskrivelse(
                        it.periodeFra,
                        it.periodeTil,
                        it.belop.negate(),
                        it.belop
                    ),
                )
            },
            krediteringFakturaRef = faktura.referanseNr,
            referertFakturaVedAvregning = faktura,
        )

        (fakturaserie.faktura as MutableList<Faktura>).add(nyFaktura)

        return fakturaserieRepository.save(fakturaserie)
    }
}