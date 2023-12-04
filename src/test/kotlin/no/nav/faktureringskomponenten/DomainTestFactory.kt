package no.nav.faktureringskomponenten

import no.nav.faktureringskomponenten.domain.models.*
import ulid.ULID
import java.math.BigDecimal
import java.time.LocalDate

object DomainTestFactory {

    class FakturaserieBuilder(
        private var referanse: String = ULID.randomULID(),
        private var fakturaGjelderInnbetalingstype: Innbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
        private var fodselsnummer: String = "01234567890",
        private var fullmektig: Fullmektig? = null,
        private var referanseBruker: String = "",
        private var referanseNAV: String = "",
        private var startdato: LocalDate = LocalDate.now(),
        private var sluttdato: LocalDate = LocalDate.now().plusMonths(6),
        private var status: FakturaserieStatus = FakturaserieStatus.OPPRETTET,
        private var intervall: FakturaserieIntervall = FakturaserieIntervall.KVARTAL,
        private val faktura: MutableList<Faktura> = mutableListOf(FakturaBuilder().build()),
        private var erstattetMed: Fakturaserie? = null,
    ) {
        fun referanse(referanse: String) = apply { this.referanse = referanse }
        fun fakturaGjelderInnbetalingstype(fakturaGjelderInnbetalingstype: Innbetalingstype) =
            apply { this.fakturaGjelderInnbetalingstype = fakturaGjelderInnbetalingstype }

        fun fodselsnummer(fodselsnummer: String) = apply { this.fodselsnummer = fodselsnummer }
        fun fullmektig(fullmektig: Fullmektig) = apply { this.fullmektig = fullmektig }
        fun referanseBruker(referanseBruker: String) = apply { this.referanseBruker = referanseBruker }
        fun referanseNAV(referanseNAV: String) = apply { this.referanseNAV = referanseNAV }
        fun startdato(startdato: LocalDate) = apply { this.startdato = startdato }
        fun sluttdato(sluttdato: LocalDate) = apply { this.sluttdato = sluttdato }
        fun status(status: FakturaserieStatus) = apply { this.status = status }
        fun intervall(intervall: FakturaserieIntervall) = apply { this.intervall = intervall }
        fun faktura(vararg faktura: Faktura) = apply {
            this.faktura.clear()
            this.faktura.addAll(faktura)
        }

        fun erstattetMed(erstattetMed: Fakturaserie) = apply { this.erstattetMed = erstattetMed }
        fun build() = Fakturaserie(
            referanse = referanse,
            fakturaGjelderInnbetalingstype = fakturaGjelderInnbetalingstype,
            fodselsnummer = fodselsnummer,
            fullmektig = fullmektig,
            referanseBruker = referanseBruker,
            referanseNAV = referanseNAV,
            startdato = startdato,
            sluttdato = sluttdato,
            status = status,
            intervall = intervall,
            faktura = faktura,
            erstattetMed = erstattetMed
        )
    }

    class FakturaBuilder(
        private var referanseNr: String = ULID.randomULID(),
        private var datoBestilt: LocalDate = LocalDate.now(),
        private var sistOppdatert: LocalDate = LocalDate.now().plusMonths(3),
        private var status: FakturaStatus = FakturaStatus.OPPRETTET,
        private var fakturaLinje: MutableList<FakturaLinje> = mutableListOf(FakturaLinjeBuilder().build()),
        private var fakturaserie: Fakturaserie? = null,
        private var eksternFakturaStatus: MutableList<EksternFakturaStatus> = mutableListOf(),
        private var eksternFakturaNummer: String = "",
        private var kreditReferanseNr: String = "",
    ) {
        fun referanseNr(referanseNr: String) = apply { this.referanseNr = referanseNr }
        fun datoBestilt(datoBestilt: LocalDate) = apply { this.datoBestilt = datoBestilt }
        fun sistOppdatert(sistOppdatert: LocalDate) = apply { this.sistOppdatert = sistOppdatert }
        fun status(status: FakturaStatus) = apply { this.status = status }
        fun fakturaLinje(vararg fakturaLinje: FakturaLinje) = apply {
            this.fakturaLinje.clear()
            this.fakturaLinje.addAll(fakturaLinje)
        }

        fun eksternFakturaStatus(vararg eksternFakturaStatus: EksternFakturaStatus) = apply {
            this.eksternFakturaStatus.clear()
            this.eksternFakturaStatus.addAll(eksternFakturaStatus)
        }

        fun eksternFakturaNummer(eksternFakturaNummer: String) =
            apply { this.eksternFakturaNummer = eksternFakturaNummer }

        fun kreditReferanseNr(kreditReferanseNr: String) = apply { this.kreditReferanseNr = kreditReferanseNr }

        fun build() = Faktura(
            referanseNr = referanseNr,
            datoBestilt = datoBestilt,
            sistOppdatert = sistOppdatert,
            status = status,
            fakturaLinje = fakturaLinje,
            fakturaserie = fakturaserie,
            eksternFakturaStatus = eksternFakturaStatus,
            eksternFakturaNummer = eksternFakturaNummer,
            kreditReferanseNr = kreditReferanseNr
        )
    }

    class FakturaLinjeBuilder(
        private var referertFakturaVedAvregning: Faktura? = null,
        private var periodeFra: LocalDate = LocalDate.now(),
        private var periodeTil: LocalDate = LocalDate.now().plusMonths(3),
        private var beskrivelse: String = "Inntekt: X, Dekning: Y, Sats: Z",
        private var antall: BigDecimal = BigDecimal(1),
        private var enhetsprisPerManed: BigDecimal = BigDecimal(10000),
        private var avregningForrigeBeloep: BigDecimal? = null,
        private var avregningNyttBeloep: BigDecimal? = null,
        private var belop: BigDecimal = BigDecimal(10000),
    ) {
        fun referertFakturaVedAvregning(referertFakturaVedAvregning: Faktura) =
            apply { this.referertFakturaVedAvregning = referertFakturaVedAvregning }

        fun periodeFra(periodeFra: LocalDate) = apply { this.periodeFra = periodeFra }
        fun periodeTil(periodeTil: LocalDate) = apply { this.periodeTil = periodeTil }
        fun beskrivelse(beskrivelse: String) = apply { this.beskrivelse = beskrivelse }
        fun antall(antall: BigDecimal) = apply { this.antall = antall }
        fun enhetsprisPerManed(enhetsprisPerManed: BigDecimal) = apply { this.enhetsprisPerManed = enhetsprisPerManed }
        fun avregningForrigeBeloep(avregningForrigeBeloep: BigDecimal) =
            apply { this.avregningForrigeBeloep = avregningForrigeBeloep }

        fun avregningNyttBeloep(avregningNyttBeloep: BigDecimal) =
            apply { this.avregningNyttBeloep = avregningNyttBeloep }

        fun belop(belop: BigDecimal) = apply { this.belop = belop }

        fun build() = FakturaLinje(
            referertFakturaVedAvregning = referertFakturaVedAvregning,
            periodeFra = periodeFra,
            periodeTil = periodeTil,
            beskrivelse = beskrivelse,
            antall = antall,
            enhetsprisPerManed = enhetsprisPerManed,
            avregningForrigeBeloep = avregningForrigeBeloep,
            avregningNyttBeloep = avregningNyttBeloep,
            belop = belop
        )
    }
}