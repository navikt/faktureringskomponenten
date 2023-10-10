package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.repositories.EksternFakturaStatusRepository
import no.nav.faktureringskomponenten.domain.repositories.FakturaRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.integration.kafka.ManglendeFakturabetalingProducer
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import no.nav.faktureringskomponenten.service.integration.kafka.dto.ManglendeFakturabetalingDto
import no.nav.faktureringskomponenten.service.mappers.EksternFakturaStatusMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

private val log = KotlinLogging.logger { }

@Service
class EksternFakturaStatusService(
    private val fakturaRepository: FakturaRepository,
    private val eksternFakturaStatusMapper: EksternFakturaStatusMapper,
    private val eksternFakturaStatusRepository: EksternFakturaStatusRepository,
    private val manglendeFakturabetalingProducer: ManglendeFakturabetalingProducer
) {

    @Transactional
    fun lagreEksternFakturaStatusMelding(eksternFakturaStatusDto: EksternFakturaStatusDto) {
        log.info("Mottatt $eksternFakturaStatusDto")

        val faktura = fakturaRepository.findById(eksternFakturaStatusDto.fakturaReferanseNr.toLong())

        faktura ?: throw RessursIkkeFunnetException(
            field = "fakturaId",
            message = "Finner ikke faktura med faktura id $eksternFakturaStatusDto.fakturaReferanseNr"
        )

        val eksternFakturaStatus = eksternFakturaStatusMapper.tilEksternFakturaStatus(eksternFakturaStatusDto)

        if(faktura.fakturaserie?.referanse != null) {
            try {
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
            } catch (e: Exception) {
                eksternFakturaStatus.apply { sendt = false }
                throw RuntimeException(
                    "Kunne ikke produsere melding om faktura mottatt bestilt for behandlingsID ${faktura.fakturaserie!!.referanse}", e
                )
            } finally {
                faktura.eksternFakturaStatus.add(eksternFakturaStatus)

                faktura.apply {
                    sistOppdatert = eksternFakturaStatusDto.dato
                    status = eksternFakturaStatusDto.status
                }

                fakturaRepository.save(faktura)
            }
        }
    }
}