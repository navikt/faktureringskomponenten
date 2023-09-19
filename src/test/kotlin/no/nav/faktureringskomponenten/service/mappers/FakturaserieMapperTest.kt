package no.nav.faktureringskomponenten.service.mappers

import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import no.nav.faktureringskomponenten.domain.models.*
import no.nav.faktureringskomponenten.service.FakturaGenerator
import no.nav.faktureringskomponenten.service.FakturalinjeGenerator
import no.nav.faktureringskomponenten.service.FakturaserieDto
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate

@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
class FakturaserieMapperTest {

    @ParameterizedTest(name = "[{index}] {2} {0}")
    @MethodSource("data")
    fun testFakturaLinjer(
        beskrivelse: String,
        dagensDato: LocalDate,
        intervall: FakturaserieIntervall,
        perioder: List<FakturaseriePeriode>,
        expected: FakturaData
    ) {
        val fakturaserie = lagFakturaserie(dagensDato, intervall, perioder)
        val result = FakturaData(fakturaserie.faktura)

        result.shouldBeEqualToComparingFields(expected)
    }

    private fun data() = listOf(
        arguments(
            "Går 1 dag inn i neste måned",
            LocalDate.of(2023, 1, 13),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 12, 1),
                    sluttDato = LocalDate.of(2023, 1, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaData(
                1,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-12-01", til = "2023-01-01",
                        listOf(
                            Linje(
                                "2022-12-01", "2022-12-31", "25470.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-01", "764.10",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    )
                )
            )
        ),

        arguments(
            "Medlemskap starter i 2022, fortsetter i 2023, overgang",
            LocalDate.of(2023, 1, 26),
            FakturaserieIntervall.KVARTAL,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2022, 6, 1),
                    sluttDato = LocalDate.of(2023, 1, 24),
                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2023, 1, 25),
                    sluttDato = LocalDate.of(2023, 6, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaData(
                2,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-06-01", til = "2023-03-31",
                        listOf(
                            Linje(
                                "2022-06-01", "2022-06-30", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-07-01", "2022-09-30", "30000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-10-01", "2022-12-31", "30000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-24", "7700.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2023-01-25", "2023-03-31", "22300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-04-01", til = "2023-06-01",
                        listOf(
                            Linje(
                                "2023-04-01", "2023-06-01", "20300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                )
            )
        ),


        arguments(
            "Slutt dato er før dagens dato",
            LocalDate.of(2023, 4, 1),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2023, 1, 1),
                    sluttDato = LocalDate.of(2023, 2, 1),
                    beskrivelse = "periode - 1"
                )
            ),
            FakturaData(
                1,
                listOf(
                    FakturaMedLinjer(
                        fra = "2023-01-01", til = "2023-02-01",
                        listOf(
                            Linje(
                                "2023-01-01", til = "2023-01-31", "10000.00",
                                "periode - 1"
                            ),
                            Linje(
                                "2023-02-01", "2023-02-01", "400.00",
                                "periode - 1"
                            ),
                        )
                    )
                )
            )
        ),

        arguments(
            "Dagens dato er lik slutt dato",
            LocalDate.of(2023, 1, 31),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 12, 1),
                    sluttDato = LocalDate.of(2023, 1, 31),
                    beskrivelse = "periode 1"
                )
            ),
            FakturaData(
                1,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-12-01", til = "2023-01-31",
                        listOf(
                            Linje(
                                "2022-12-01", "2022-12-31", "25470.00",
                                "periode 1"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-31", "25470.00",
                                "periode 1"
                            ),
                        )
                    )
                )
            )
        ),

        arguments(
            "Før og etter dagens dato",
            LocalDate.of(2023, 1, 13),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 12, 1),
                    sluttDato = LocalDate.of(2023, 2, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaData(
                2,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-12-01", til = "2023-01-31",
                        listOf(
                            Linje(
                                "2022-12-01", "2022-12-31", "25470.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-31", "25470.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),

                            )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-02-01", til = "2023-02-01",
                        listOf(
                            Linje(
                                "2023-02-01", "2023-02-01", "1018.80",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            )
                        )
                    )
                )
            )
        ),

        arguments(
            "2 faktura perioder - kun 1 dag i siste måned",
            LocalDate.of(2023, 1, 23),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 12, 1),
                    sluttDato = LocalDate.of(2023, 1, 22),
                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2023, 1, 23),
                    sluttDato = LocalDate.of(2023, 2, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaData(
                2,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-12-01", til = "2023-01-31",
                        listOf(
                            Linje(
                                "2022-12-01", "2022-12-31", "25470.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-22", "18083.70",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2023-01-23", "2023-01-31", "7386.30",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-02-01", til = "2023-02-01",
                        listOf(
                            Linje(
                                "2023-02-01", "2023-02-01", "1018.80",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                )
            )
        ),

        arguments(
            "2 perioder - lag 6 faktura med linjer",
            LocalDate.of(2023, 1, 26),
            FakturaserieIntervall.MANEDLIG,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2022, 6, 1),
                    sluttDato = LocalDate.of(2023, 1, 24),
                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2023, 1, 25),
                    sluttDato = LocalDate.of(2023, 6, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaData(
                6,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-06-01", til = "2023-01-31",
                        listOf(
                            Linje(
                                "2022-06-01", "2022-06-30", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-07-01", "2022-07-31", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-08-01", "2022-08-31", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-09-01", "2022-09-30", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-10-01", "2022-10-31", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-11-01", "2022-11-30", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-12-01", "2022-12-31", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-24", "7700.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2023-01-25", "2023-01-31", "2300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-02-01", til = "2023-02-28",
                        listOf(
                            Linje(
                                "2023-02-01", "2023-02-28", "10000.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-03-01", til = "2023-03-31",
                        listOf(
                            Linje(
                                "2023-03-01", "2023-03-31", "10000.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-04-01", til = "2023-04-30",
                        listOf(
                            Linje(
                                "2023-04-01", "2023-04-30", "10000.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-05-01", til = "2023-05-31",
                        listOf(
                            Linje(
                                "2023-05-01", "2023-05-31", "10000.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-06-01", til = "2023-06-01",
                        listOf(
                            Linje(
                                "2023-06-01", "2023-06-01", "300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                )
            )
        ),

        arguments(
            "2 perioder - lag 2 faktura med linjer",
            LocalDate.of(2023, 1, 26),
            FakturaserieIntervall.KVARTAL,
            listOf(
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2022, 6, 1),
                    sluttDato = LocalDate.of(2023, 1, 24),
                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                ),
                FakturaseriePeriode(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2023, 1, 25),
                    sluttDato = LocalDate.of(2023, 6, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            FakturaData(
                2,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-06-01", til = "2023-03-31",
                        listOf(
                            Linje(
                                "2022-06-01", "2022-06-30", "10000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-07-01", "2022-09-30", "30000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-10-01", "2022-12-31", "30000.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-24", "7700.00",
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2023-01-25", "2023-03-31", "22300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-04-01", til = "2023-06-01",
                        listOf(
                            Linje(
                                "2023-04-01", "2023-06-01", "20300.00",
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    ),
                )
            )
        )
    )

    private fun lagFakturaserie(
        dagensDato: LocalDate = LocalDate.now(),
        intervall: FakturaserieIntervall = FakturaserieIntervall.MANEDLIG,
        perioder: List<FakturaseriePeriode> = listOf()
    ): Fakturaserie {
        val fakturaMapper = FakturaGeneratorForTest(dagensDato)
        return FakturaserieMapper(fakturaMapper).tilFakturaserie(
            FakturaserieDto(
                fakturaserieReferanse = "MEL-105-145",
                fodselsnummer = "30056928150",
                fullmektig = Fullmektig(
                    fodselsnummer = null,
                    organisasjonsnummer = "999999999",
                    kontaktperson = "Test person"
                ),
                referanseBruker = "2023-01-19T11:39:48.680364Z", // Hvorfor får vi dagens dato her?
                referanseNAV = "Medlemskap og avgift",
                fakturaGjelderInnbetalingstype = Innbetalingstype.TRYGDEAVGIFT,
                intervall = intervall,
                perioder = perioder
            )
        )
    }

    class FakturaGeneratorForTest(private val dagensDato: LocalDate) : FakturaGenerator(FakturalinjeGenerator()) {
        override fun dagensDato(): LocalDate = dagensDato
    }

    data class FakturaData(
        val size: Int,
        val fakturaMedLinjer: List<FakturaMedLinjer>

    ) {
        constructor(fakturaListe: List<Faktura>) :
                this(fakturaListe.size,
                    fakturaListe.map { f ->
                        FakturaMedLinjer(
                            f.getPeriodeFra(),
                            f.getPeriodeTil(),
                            f.fakturaLinje.map { fl ->
                                Linje(
                                    fl.periodeFra,
                                    fl.periodeTil,
                                    fl.belop,
                                    fl.beskrivelse
                                )
                            }
                        )
                    })

        override fun toString() = "size=$size fakturaListe=$fakturaMedLinjer\n"

        fun toTestCode(): String =
            "FakturaData(\n" +
                    "  $size,\n  listOf(\n" +
                    fakturaMedLinjer.joinToString("\n") { it.toTestCode() } +
                    "\n )" +
                    "\n)"

    }

    data class FakturaMedLinjer(
        val fra: LocalDate,
        val til: LocalDate,
        val fakturaLinjer: List<Linje>
    ) {
        constructor(fra: String, til: String, fakturaLinjer: List<Linje>) :
                this(LocalDate.parse(fra), LocalDate.parse(til), fakturaLinjer)

        override fun toString() = "\n" +
                "  fra:$fra, til:$til fakturaLinjer:$fakturaLinjer\n"

        fun toTestCode(): String = "    FakturaMedLinjer(\n" +
                "      fra = \"$fra\", til = \"$til\",\n" +
                "      listOf(\n" +
                "${fakturaLinjer.joinToString("\n") { it.toTestCode() }}\n" +
                "      )\n" +
                "  ),"
    }

    data class Linje(
        val fra: LocalDate,
        val til: LocalDate,
        val beløp: BigDecimal,
        val beskrivelse: String,
    ) {

        constructor(fra: String, til: String, beløp: String, beskrivelse: String)
                : this(LocalDate.parse(fra), LocalDate.parse(til), BigDecimal(beløp), beskrivelse)

        override fun toString() = "\n    fra=$fra, til:$til, beløp:$beløp, $beskrivelse"
        fun toTestCode(): String = "           Linje(\n" +
                "             \"$fra\", \"$til\", $beløp,\n " +
                "             \"$beskrivelse\"\n" +
                "            ),"
    }
}
