package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.FakturaBestiltProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltLinjeDto
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaMottattDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Component
class FakturaService(
    private val fakturaRepository: FakturaRepository,
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaBestiltProducer: FakturaBestiltProducer,
) {
    private val log: Logger = LoggerFactory.getLogger(FakturaService::class.java)

    fun hentBestillingsklareFaktura(bestillingsDato: LocalDate = LocalDate.now()): List<Faktura> =
        fakturaRepository.findAllByDatoBestiltIsLessThanEqualAndStatusIsOpprettet(bestillingsDato)

    fun lagreFakturaMottattMelding(fakturaMottattDto: FakturaMottattDto) {
        val faktura = fakturaRepository.findById(fakturaMottattDto.fakturaReferanseNr.toLong())
            ?: throw RessursIkkeFunnetException(
                field = "fakturaId",
                message = "Finner ikke faktura med faktura id $fakturaMottattDto.fakturaReferanseNr"
            )

        if (faktura.status == FakturaStatus.BESTILLT) {
            faktura.apply {
                status = fakturaMottattDto.status
                innbetaltBelop = fakturaMottattDto.belop
            }

            fakturaRepository.save(faktura)
            log.info("Faktura {} er endret til {}", faktura.id, faktura)
        }
    }

    @Transactional
    fun bestillFaktura(fakturaId: Long) {
        val faktura = fakturaRepository.findById(fakturaId) ?: throw RessursIkkeFunnetException(
            field = "fakturaId",
            message = "Finner ikke faktura med faktura id $fakturaId"
        )

        val fakturaserieId = faktura.getFakturaserieId()
            ?: throw RessursIkkeFunnetException(
                field = "fakturaserieId",
                message = "Finner ikke fakturaserie med faktura id ${faktura.id}"
            )

        val fakturaserie = fakturaserieRepository.findById(fakturaserieId) ?: throw RessursIkkeFunnetException(
            field = "fakturaserieId",
            message = "Finner ikke fakturaserie med fakturaserieId $fakturaserieId"
        )

        faktura.status = FakturaStatus.BESTILLT
        fakturaserie.status = FakturaserieStatus.UNDER_BESTILLING

        val fakturaBestiltDto = FakturaBestiltDto(
            fodselsnummer = fakturaserie.fodselsnummer,
            fullmektigOrgnr = fakturaserie.fullmektig?.organisasjonsnummer,
            fullmektigFnr = fakturaserie.fullmektig?.fodselsnummer,
            vedtaksId = fakturaserie.vedtaksId,
            fakturaReferanseNr = "${faktura.id}",
            kreditReferanseNr = "",
            referanseBruker = fakturaserie.referanseBruker,
            referanseNAV = fakturaserie.referanseNAV,
            beskrivelse = fakturaserie.fakturaGjelder,
            faktureringsDato = faktura.datoBestilt,
            fakturaLinjer = faktura.fakturaLinje.map {
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                val periodeFraFormatert = it.periodeFra.format(formatter)
                val periodeTilFormatert = it.periodeTil.format(formatter)

                FakturaBestiltLinjeDto(
                    beskrivelse = "Periode: $periodeFraFormatert - ${periodeTilFormatert}, ${it.beskrivelse}",
                    antall = 1.0,
                    enhetspris = it.enhetsprisPerManed,
                    belop = it.belop
                )
            },
        )

        fakturaBestiltProducer.produserBestillingsmelding(fakturaBestiltDto)

        fakturaserieRepository.save(fakturaserie)
        fakturaRepository.save(faktura)
    }
}
