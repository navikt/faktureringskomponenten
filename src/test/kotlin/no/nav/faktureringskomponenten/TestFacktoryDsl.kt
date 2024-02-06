package no.nav.faktureringskomponenten

import no.nav.faktureringskomponenten.domain.models.*
import ulid.ULID
import java.math.BigDecimal
import java.time.LocalDate

@DslMarker
annotation class TestdataDsl


fun lagFakturaserie(block: FakturaserieBuilder.() -> Unit): Fakturaserie =
    FakturaserieBuilder().apply(block).build()

@TestdataDsl
class FakturaserieBuilder(
    private var id: Long? = null,
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
    val faktura: MutableList<Faktura> = mutableListOf(FakturaBuilder().build()),
    private var erstattetMed: Fakturaserie? = null,
) {
    fun id(id: Long) = apply { this.id = id }
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
        id = id,
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

fun lagFaktura(block: FakturaBuilder.() -> Unit): Faktura =
    FakturaBuilder().apply(block).build()

@TestdataDsl
class FakturaBuilder(
    private var referanseNr: String = ULID.randomULID(),
    private var datoBestilt: LocalDate = LocalDate.now(),
    private var status: FakturaStatus = FakturaStatus.OPPRETTET,
    private var fakturaLinje: MutableList<FakturaLinje> = mutableListOf(FakturaLinjeBuilder().build()),
    private var fakturaserie: Fakturaserie? = null,
    private var eksternFakturaStatus: MutableList<EksternFakturaStatus> = mutableListOf(),
    private var eksternFakturaNummer: String = "",
    private var krediteringFakturaRef: String = "",
    private var referertFakturaVedAvregning: Faktura? = null
) {
    fun referanseNr(referanseNr: String) = apply { this.referanseNr = referanseNr }
    fun datoBestilt(datoBestilt: LocalDate) = apply { this.datoBestilt = datoBestilt }
    fun status(status: FakturaStatus) = apply { this.status = status }
    fun fakturaLinje(vararg fakturaLinje: FakturaLinje) = apply {
        this.fakturaLinje.clear()
        this.fakturaLinje.addAll(fakturaLinje)
    }

    fun fakturaserie(fakturaserie: Fakturaserie) = apply { this.fakturaserie = fakturaserie }

    fun eksternFakturaStatus(vararg eksternFakturaStatus: EksternFakturaStatus) = apply {
        this.eksternFakturaStatus.clear()
        this.eksternFakturaStatus.addAll(eksternFakturaStatus)
    }

    fun eksternFakturaNummer(eksternFakturaNummer: String) =
        apply { this.eksternFakturaNummer = eksternFakturaNummer }

    fun krediteringFakturaRef(krediteringFakturaRef: String) = apply { this.krediteringFakturaRef = krediteringFakturaRef }

    fun referertFakturaVedAvregning(referertFakturaVedAvregning: Faktura) =
        apply { this.referertFakturaVedAvregning = referertFakturaVedAvregning }

    fun build() = Faktura(
        referanseNr = referanseNr,
        datoBestilt = datoBestilt,
        status = status,
        fakturaLinje = fakturaLinje,
        fakturaserie = fakturaserie,
        eksternFakturaStatus = eksternFakturaStatus,
        eksternFakturaNummer = eksternFakturaNummer,
        krediteringFakturaRef = krediteringFakturaRef,
        referertFakturaVedAvregning = referertFakturaVedAvregning
    )
}

fun lagFakturalinje(block: FakturaLinjeBuilder.() -> Unit): FakturaLinje =
    FakturaLinjeBuilder().apply(block).build()

@TestdataDsl
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