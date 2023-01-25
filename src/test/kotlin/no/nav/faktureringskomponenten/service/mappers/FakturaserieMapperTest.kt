package no.nav.faktureringskomponenten.service.mappers

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import no.nav.faktureringskomponenten.controller.dto.FakturaserieDto
import no.nav.faktureringskomponenten.controller.dto.FakturaserieIntervallDto
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.FullmektigDto
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import org.junit.jupiter.api.Test
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
        intervall: FakturaserieIntervallDto,
        perioder: List<FakturaseriePeriodeDto>,
        expected: Expected.FakturaData
    ) {
        val fakturaserie = lagFakturaserie(intervall, perioder)
        val result = Expected.FakturaData(fakturaserie.faktura)
        print("result=$result")

        result.shouldBeEqualToComparingFields(expected)
    }

    private fun data() = listOf(
        arguments(
            FakturaserieIntervallDto.MANEDLIG,
            listOf(
                FakturaseriePeriodeDto(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 12, 1),
                    sluttDato = LocalDate.of(2023, 1, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            Expected.FakturaData(
                1,
                listOf(
                    Expected.FakturaMedLinjer(
                        fra = "2022-12-01", til = "2023-01-01",
                        listOf(
                            Expected.Linje(
                                "2022-12-01", "2022-12-31", 25470,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Expected.Linje(
                                "2023-01-01", "2023-01-01", 821,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                        )
                    )
                )
            )

        ),
        arguments(
            FakturaserieIntervallDto.MANEDLIG,
            listOf(
                FakturaseriePeriodeDto(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 12, 1),
                    sluttDato = LocalDate.of(2023, 2, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            ),
            Expected.FakturaData(
                2,
                listOf(
                    Expected.FakturaMedLinjer(
                        fra = "2022-12-01", til = "2023-01-31",
                        listOf(
                            Expected.Linje(
                                "2022-12-01", "2022-12-31", 25470,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),
                            Expected.Linje(
                                "2023-01-01", "2023-01-31", 25470,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            ),

                            )
                    ),
                    Expected.FakturaMedLinjer(
                        fra = "2023-02-01", til = "2023-02-01",
                        listOf(
                            Expected.Linje(
                                "2023-02-01", "2023-02-01", 909,
                                "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                            )
                        )
                    )
                )
            )
        )
    )

    @Test // Fjerne denne etter at ting er flyttet til test over
    fun test() {

        val fakturaserie: Fakturaserie = lagFakturaserie(
            intervall = FakturaserieIntervallDto.MANEDLIG,
            perioder = listOf(
                FakturaseriePeriodeDto(
                    enhetsprisPerManed = BigDecimal(25470),
                    startDato = LocalDate.of(2022, 12, 1),
                    sluttDato = LocalDate.of(2023, 2, 1),
                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
                )
            )

//            perioder = listOf(
//                FakturaseriePeriodeDto(
//                    enhetsprisPerManed = BigDecimal(25470),
//                    startDato = LocalDate.of(2023, 1, 23),
//                    sluttDato = LocalDate.of(2023, 2, 1),
//                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
//                ),
//                FakturaseriePeriodeDto(
//                    enhetsprisPerManed = BigDecimal(25470),
//                    startDato = LocalDate.of(2022, 12, 1),
//                    sluttDato = LocalDate.of(2023, 1, 22),
//                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
//                )

//            perioder = listOf(
//                FakturaseriePeriodeDto(
//                    enhetsprisPerManed = BigDecimal(25470),
//                    startDato = LocalDate.of(2022, 1, 19),
//                    sluttDato = LocalDate.of(2023, 5, 1),
//                    beskrivelse = "Inntekt: 90000, Dekning: HELSE_OG_PENSJONSDEL, Sats: 28.3 %"
//                ),
//                FakturaseriePeriodeDto(
//                    enhetsprisPerManed = BigDecimal(25470),
//                    startDato = LocalDate.of(2022, 1, 1),
//                    sluttDato = LocalDate.of(2023, 5, 18),
//                    beskrivelse = "Inntekt: 100000, Dekning: PENSJONSDEL, Sats: 21.5 %"
//                )
//            )
        )



        println("fakturaserie.faktura.size ${fakturaserie.faktura.size}")
        fakturaserie.faktura.forEach { f ->
            println("======================================================")
            println("faktura.periode fra: ${f.getPeriodeFra()}, til: ${f.getPeriodeTil()}, fakturaLinje.size: ${f.fakturaLinje.size}")
            println("------------------------------------------------------")
            f.fakturaLinje.forEach { fi ->
                println("fakturaLinje.periodeFra: ${fi.periodeFra},  fakturaLinje.periodeTil:${fi.periodeTil}, beløp: ${fi.belop}, beskrivelse:${fi.beskrivelse}")
            }
        }

        val json = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(fakturaserie)
        println(json)
    }

    private fun lagFakturaserie(
        intervall: FakturaserieIntervallDto = FakturaserieIntervallDto.MANEDLIG,
        perioder: List<FakturaseriePeriodeDto> = listOf()
    ): Fakturaserie {
        val fakturalinjeMapper = FakturalinjeMapper()
        val fakturaMapper = FakturaMapper(fakturalinjeMapper)
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

    class Expected {
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
        }

        data class FakturaMedLinjer(
            val fra: LocalDate,
            val til: LocalDate,
            val fakturaLinjer: List<Linje>
        ) {
            constructor(fra: String, til: String, fakturaLinjer: List<Linje>) :
                    this(LocalDate.parse(fra), LocalDate.parse(til), fakturaLinjer)

            override fun toString() = "\n  fra:$fra, til:$til fakturaLinjer:$fakturaLinjer\n"
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
        }
    }
}
