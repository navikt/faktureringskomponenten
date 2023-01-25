package no.nav.faktureringskomponenten.service.mappers

import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieIntervallDto
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.FullmektigDto
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate

@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
class FakturaserieMapperTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("data")
    fun testFakturaLinjer(
        beskrivelse: String,
        dagensDato: LocalDate,
        intervall: FakturaserieIntervallDto,
        perioder: List<FakturaseriePeriodeDto>,
        expected: FakturaData
    ) {
        val fakturaserie = lagFakturaserie(dagensDato, intervall, perioder)
        val result = FakturaData(fakturaserie.faktura)
        print(result.toString())

        result.shouldBeEqualToComparingFields(expected)
    }

    private fun data() = listOf(
        arguments(
            "Før dagens data",
            LocalDate.now(),
            FakturaserieIntervallDto.MANEDLIG,
            listOf(
                FakturaseriePeriodeDto(
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
                                "2022-12-01", "2022-12-31", 25470,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-01", 821,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    )
                )
            )

        ),
        arguments(
            "Før og etter dagens dato",
            LocalDate.now(),
            FakturaserieIntervallDto.MANEDLIG,
            listOf(
                FakturaseriePeriodeDto(
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
                                "2022-12-01", "2022-12-31", 25470,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-31", 25470,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),

                            )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-02-01", til = "2023-02-01",
                        listOf(
                            Linje(
                                "2023-02-01", "2023-02-01", 909,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            )
                        )
                    )
                )
            )
        ),
        arguments(
            "2 faktura serier - lager 2 faktura med linjer",
            LocalDate.now(),
            FakturaserieIntervallDto.MANEDLIG,
            listOf(
                FakturaseriePeriodeDto(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2023, 1, 23),
                    sluttDato = LocalDate.of(2023, 2, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriodeDto(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 12, 1),
                    sluttDato = LocalDate.of(2023, 1, 22),
                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                )
            ),
            FakturaData(
                2,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-12-01", til = "2023-01-31",
                        listOf(
                            Linje(
                                "2022-12-01", "2022-12-31", 25470,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2023-01-23", "2023-01-31", 7394,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-22", 18075,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-02-01", til = "2023-02-01",
                        listOf(
                            Linje(
                                "2023-02-01",
                                "2023-02-01",
                                909,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje( // TODO: dette virker feil. Må muligens mocke dagens dato
                                "2023-02-01", "2023-01-22", -8304, "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                        )
                    ),
                )
            )
        ),

        arguments(
            "2 faktura serier - lager 5 faktura med linjer",
            LocalDate.now(),
            FakturaserieIntervallDto.MANEDLIG,
            listOf(
                FakturaseriePeriodeDto(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2023, 1, 19),
                    sluttDato = LocalDate.of(2023, 5, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                ),
                FakturaseriePeriodeDto(
                    enhetsprisPerManed = BigDecimal(10000),
                    startDato = LocalDate.of(2022, 1, 1),
                    sluttDato = LocalDate.of(2023, 5, 18),
                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                )
            ),
            FakturaData(
                5,
                listOf(
                    FakturaMedLinjer(
                        fra = "2022-01-01", til = "2023-01-31",
                        listOf(
                            Linje(
                                "2022-01-01", "2022-01-31", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-02-01", "2022-02-28", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-03-01", "2022-03-31", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-04-01", "2022-04-30", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-05-01", "2022-05-31", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-06-01", "2022-06-30", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-07-01", "2022-07-31", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-08-01", "2022-08-31", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-09-01", "2022-09-30", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-10-01", "2022-10-31", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-11-01", "2022-11-30", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2022-12-01", "2022-12-31", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                            Linje(
                                "2023-01-19", "2023-01-31", 4193,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-01-01", "2023-01-31", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-02-01", til = "2023-02-28",
                        listOf(
                            Linje(
                                "2023-02-01", "2023-02-28", 10000,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-02-01", "2023-02-28", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-03-01", til = "2023-03-31",
                        listOf(
                            Linje(
                                "2023-03-01", "2023-03-31", 10000,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-03-01", "2023-03-31", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-04-01", til = "2023-04-30",
                        listOf(
                            Linje(
                                "2023-04-01", "2023-04-30", 10000,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Linje(
                                "2023-04-01", "2023-04-30", 10000,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                        )
                    ),
                    FakturaMedLinjer(
                        fra = "2023-05-01", til = "2023-05-18",
                        listOf(
                            Linje(
                                "2023-05-01", "2023-05-18", 5806,
                                "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
                            ),
                        )
                    ),
                )
            )
        )

    )

    private fun lagFakturaserie(
        dagensDato: LocalDate = LocalDate.now(),
        intervall: FakturaserieIntervallDto = FakturaserieIntervallDto.MANEDLIG,
        perioder: List<FakturaseriePeriodeDto> = listOf()
    ): Fakturaserie {
        val fakturaMapper = FakturaMapperForTest(dagensDato)
        return FakturaserieMapper(fakturaMapper).tilFakturaserie(
            FakturaserieDto(
                vedtaksId = "MEL-105-145",
                fodselsnummer = "30056928150",
                fullmektig = FullmektigDto(
                    fodselsnummer = null,
                    organisasjonsnummer = "999999999",
                    kontaktperson = "Test person"
                ),
                referanseBruker = "2023-01-19T11:39:48.680364Z", // Hvorfor får vi dagens dato her?
                referanseNAV = "Medlemskap og avgift",
                fakturaGjelder = "Medlemskapsavgift",
                intervall = intervall,
                perioder = perioder
            )
        )
    }

    class FakturaMapperForTest(private val dagensDato: LocalDate) : FakturaMapper(FakturalinjeMapper()) {
        override fun dagensDato(): LocalDate = dagensDato
    }

    //        println(fakturaserie)
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
                                    fl.belop.toInt(),
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
        val belop: Int,
        val beskrivelse: String,
    ) {

        constructor(fra: String, til: String, belop: Int, beskrivelse: String)
                : this(LocalDate.parse(fra), LocalDate.parse(til), belop, beskrivelse)

        override fun toString() = "\n    fra=$fra, til:$til, beløp:$belop, $beskrivelse"
        fun toTestCode(): String = "           Linje(\n" +
                "             \"$fra\", \"$til\", $belop,\n " +
                "             \"$beskrivelse\"\n" +
                "            ),"
    }
}
