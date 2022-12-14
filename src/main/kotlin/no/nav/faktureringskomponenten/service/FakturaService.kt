package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltLinjeDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Component
class FakturaService(
    @Autowired val fakturaRepository: FakturaRepository,
    @Autowired val fakturaserieRepository: FakturaserieRepository,
    @Autowired val fakturaBestiltProducer: FakturaBestiltProducer,
) {

    fun hentBestillingsklareFaktura(bestillingsDato: LocalDate = LocalDate.now()): List<Faktura> {
        return fakturaRepository.findAllByDatoBestiltIsLessThanEqualAndStatusIs(bestillingsDato)
    }

    @Transactional
    fun bestillFaktura(fakturaId: Long) {
        val faktura = fakturaRepository.findById(fakturaId)
        val fakturaserie = faktura.fakturaserie
        faktura.status = FakturaStatus.BESTILLT
        fakturaserie.status = FakturaserieStatus.UNDER_BESTILLING

        val fakturaBestiltDto = FakturaBestiltDto(
            fodselsnummer = fakturaserie.fodselsnummer,
            fullmektigOrgnr = fakturaserie.fullmektig?.organisasjonsnummer ?: "",
            fullmektigFnr = fakturaserie.fullmektig?.fodselsnummer,
            vedtaksnummer = fakturaserie.vedtaksId,
            fakturaReferanseNr = "",
            kreditReferanseNr = "",
            referanseBruker = fakturaserie.referanseBruker ?: "",
            referanseNAV = fakturaserie.referanseNAV,
            beskrivelse = fakturaserie.fakturaGjelder,
            fakturaLinjer = faktura.fakturaLinje.map {
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                val periodeFraFormatert = it.periodeFra.format(formatter)
                val periodeTilFormatert = it.periodeTil.format(formatter)

                FakturaBestiltLinjeDto(
                    beskrivelse = "Periode: $periodeFraFormatert - ${periodeTilFormatert}, ${it.beskrivelse}",
                    antall = 1.0,
                    enhetspris = it.belop,
                    belop = it.belop
                )
            },
            faktureringsDato = faktura.datoBestilt
        )

        fakturaBestiltProducer.produserBestillingsmelding(fakturaBestiltDto)

        fakturaserieRepository.save(fakturaserie)
        fakturaRepository.save(faktura)
    }
}