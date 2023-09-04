package no.nav.faktureringskomponenten.service

import mu.KotlinLogging
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.exceptions.RessursIkkeFunnetException
import no.nav.faktureringskomponenten.service.mappers.FakturaserieMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Component
class FakturaserieService(
    private val fakturaserieRepository: FakturaserieRepository,
    private val fakturaserieMapper: FakturaserieMapper,
) {

    fun hentFakturaserie(referanseId: String): Fakturaserie =
        fakturaserieRepository.findByReferanseId(referanseId) ?: throw RessursIkkeFunnetException(
            field = "referanseId",
            message = "Fant ikke fakturaserie på: $referanseId"
        )

    fun hentFakturaserier(referanseId: String, fakturaStatus: String?): List<Fakturaserie> {
        return fakturaserieRepository.findAllByReferanseId2(referanseId, fakturaStatus)
    }

    @Transactional
    fun lagNyFakturaserie(fakturaserieDto: FakturaserieDto, forrigeReferanseId: String? = null): String {
        if(!forrigeReferanseId.isNullOrEmpty() && fakturaserieRepository.findFakturaserieByReferanseIdAndStatusIn(forrigeReferanseId) != null){
            endreFakturaserie(forrigeReferanseId, fakturaserieDto);
            log.info("Kansellerer fakturaserie: ${fakturaserieDto.referanseId}, lager ny fakturaserie: ${fakturaserieDto.referanseId}")
            return fakturaserieDto.referanseId
        }

        val fakturaserie = fakturaserieMapper.tilFakturaserie(fakturaserieDto)
        fakturaserieRepository.save(fakturaserie)
        log.info("Lagret fakturaserie: $fakturaserie")
        return fakturaserie.referanseId
    }

    @Transactional
    fun endreFakturaserie(opprinneligReferanseId: String, fakturaserieDto: FakturaserieDto) {
        val opprinneligFakturaserie = fakturaserieRepository.findFakturaserieByReferanseIdAndStatusIn(opprinneligReferanseId)
            ?: throw RessursIkkeFunnetException(
                field = "referanseId",
                message = "Fant ikke opprinnelig fakturaserie med referanseId $opprinneligReferanseId"
            )

        val opprinneligFakturaserieErUnderBestilling =
            opprinneligFakturaserie.status == FakturaserieStatus.UNDER_BESTILLING

        val fakturaSomIkkeErSendt = opprinneligFakturaserie.faktura.filter { it.status == FakturaStatus.OPPRETTET }
            .sortedBy { it.getPeriodeFra() }

        val fakturaSomIkkeErSendtPeriodeFra =
            if (fakturaSomIkkeErSendt.isNotEmpty()) fakturaSomIkkeErSendt[0].getPeriodeFra() else null

        val nyFakturaserie =
            fakturaserieMapper.tilFakturaserie(
                fakturaserieDto,
                if (opprinneligFakturaserieErUnderBestilling) fakturaSomIkkeErSendtPeriodeFra else null
            )

        opprinneligFakturaserie.status = FakturaserieStatus.ERSTATTET
        fakturaSomIkkeErSendt.forEach { it.status = FakturaStatus.KANSELLERT }

        nyFakturaserie.apply { erstattetMed = opprinneligFakturaserie.id }

        fakturaserieRepository.save(opprinneligFakturaserie)
        fakturaserieRepository.save(nyFakturaserie)
        log.info("Kansellert fakturaserie med id: ${opprinneligFakturaserie.referanseId}, lager ny med id: ${nyFakturaserie.referanseId}")
    }

    fun finnesReferanseId(referanseId: String): Boolean {
        return fakturaserieRepository.findByReferanseId(referanseId) != null
    }
}
