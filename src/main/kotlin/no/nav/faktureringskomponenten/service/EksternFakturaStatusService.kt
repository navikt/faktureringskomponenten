package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.EksternFakturaStatus
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.exceptions.EksternFeilException
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.integration.kafka.ManglendeFakturabetalingProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.ManglendeFakturabetalingDto
import no.nav.faktureringskomponenten.service.mappers.EksternFakturaStatusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode

private val log = KotlinLogging.logger { }

@Service
class EksternFakturaStatusService(
    private val fakturaRepository: FakturaRepository,
    private val eksternFakturaStatusMapper: EksternFakturaStatusMapper,
    private val manglendeFakturabetalingProducer: ManglendeFakturabetalingProducer
) {

    @Transactional
    fun lagreEksternFakturaStatusMelding(eksternFakturaStatusDto: EksternFakturaStatusDto) {
        log.info("Mottatt $eksternFakturaStatusDto")

        val faktura = fakturaRepository.findByReferanseNr(eksternFakturaStatusDto.fakturaReferanseNr)
        faktura ?: throw RessursIkkeFunnetException(
            field = "faktura.referanseNr",
            message = "Finner ikke faktura med faktura referanse nr ${eksternFakturaStatusDto.fakturaReferanseNr}"
        )

        val eksternFakturaStatus = eksternFakturaStatusMapper.tilEksternFakturaStatus(eksternFakturaStatusDto, faktura)

        produserBestillingsmeldingOgOppdater(faktura, eksternFakturaStatus, eksternFakturaStatusDto)
    }

    private fun produserBestillingsmeldingOgOppdater(faktura: Faktura, eksternFakturaStatus: EksternFakturaStatus, eksternFakturaStatusDto: EksternFakturaStatusDto){
        try {
            if (erDuplikat(faktura, eksternFakturaStatus)) {
                lagreFaktura(faktura, eksternFakturaStatusDto)
                return
            }

            if (eksternFakturaStatus.status == FakturaStatus.FEIL) {
                log.error("EksternFakturaStatus er FEIL. Gjelder faktura: ${faktura.referanseNr}. Feilmelding fra OEBS: ${eksternFakturaStatus.feilMelding}")
            }

            if(eksternFakturaStatus.status == FakturaStatus.MANGLENDE_INNBETALING) {
                manglendeFakturabetalingProducer.produserBestillingsmelding(
                    ManglendeFakturabetalingDto(
                        fakturaserieReferanse = faktura.fakturaserie!!.referanse,
                        mottaksDato = eksternFakturaStatus.dato!!
                    )
                )
                eksternFakturaStatus.apply { sendt = true }
            } else {
                eksternFakturaStatus.apply { sendt = false }
            }

            faktura.eksternFakturaStatus.add(eksternFakturaStatus)

            lagreFaktura(faktura, eksternFakturaStatusDto)

        } catch (e: Exception) {
            eksternFakturaStatus.apply { sendt = false }
            throw RuntimeException(
                "Kunne ikke produsere melding om faktura mottatt bestilt for fakturaserieReferanse ${faktura.fakturaserie!!.referanse}", e
            )
        }
    }

    private fun lagreFaktura(faktura: Faktura, eksternFakturaStatusDto: EksternFakturaStatusDto) {
        faktura.apply {
            eksternFakturaNummer = eksternFakturaStatusDto.fakturaNummer ?: ""
            sistOppdatert = eksternFakturaStatusDto.dato
            status = eksternFakturaStatusDto.status
        }

        fakturaRepository.save(faktura)
    }

    private fun erDuplikat(
        faktura: Faktura,
        eksternFakturaStatus: EksternFakturaStatus
    ): Boolean {
        if (faktura.eksternFakturaStatus.any {
                val erDuplikatEksternFakturaStatusFeil = (it.status == eksternFakturaStatus.status
                        && faktura.status == eksternFakturaStatus.status
                        && it.faktura?.id == eksternFakturaStatus.faktura?.id
                        && it.feilMelding == eksternFakturaStatus.feilMelding)

                val erDuplikatEksternFakturaStatus = (it.status == eksternFakturaStatus.status
                        && it.fakturaBelop == eksternFakturaStatus.fakturaBelop?.setScale(2, RoundingMode.DOWN)
                        && it.ubetaltBelop == eksternFakturaStatus.ubetaltBelop?.setScale(2, RoundingMode.DOWN)
                        && it.faktura?.id == eksternFakturaStatus.faktura?.id)

                erDuplikatEksternFakturaStatusFeil || erDuplikatEksternFakturaStatus
            }) {
            log.info("EksternFakturaStatus er duplikat, ikke lagre med referanse: {}", faktura.referanseNr)
            return true
        }
        return false
    }
}