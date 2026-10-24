package no.nav.faktureringskomponenten.statistikk

import io.kotest.assertions.assertSoftly
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.faktureringskomponenten.controller.AdminControllerIT
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.forTest
import no.nav.faktureringskomponenten.domain.models.EksternFakturaStatus
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.models.FakturaTestFactory
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import no.nav.faktureringskomponenten.domain.models.forTest
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.EmbeddedKafkaBase
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.testcontainers.junit.jupiter.Testcontainers
import ulid.ULID
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Testcontainers
@ActiveProfiles("itest", "embeded-kafka")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
class StatistikkIT(
    @param:Autowired private val webClient: WebTestClient,
    @param:Autowired private val server: MockOAuth2Server,
    @param:Autowired private val fakturaserieRepository: FakturaserieRepository,
    @param:Autowired private val jdbc: JdbcTemplate,
    @param:Autowired private val statistikkService: StatistikkService,
    @param:Value("\${admin.console-klient-id}") private val consoleKlientId: String,
) : EmbeddedKafkaBase(fakturaserieRepository) {

    @BeforeEach
    fun registrerOpprydding() {
        addCleanUpAction {
            jdbc.execute("DELETE FROM ekstern_faktura_status")
            jdbc.execute("DELETE FROM faktura_linje")
            jdbc.execute("UPDATE faktura SET avregning_faktura_id = NULL")
            jdbc.execute("DELETE FROM faktura")
            jdbc.execute("UPDATE fakturaserie SET erstattet_med = NULL")
            jdbc.execute("DELETE FROM fakturaserie")
            jdbc.execute("DELETE FROM faktura_mottak_feil")
        }
    }

    @Test
    fun `regner ut kvartalskjøringer og nåtilstand`() {
        val (serieA, serieB) = lagTestdata()

        val bilde = statistikkService.hentOyeblikksbilde(NAA)

        bilde.idag shouldBe LocalDate.of(2026, 10, 23)
        bilde.kjoeringer.map { it.dato } shouldContainExactly listOf(
            LocalDate.of(2026, 3, 19), LocalDate.of(2026, 6, 19), LocalDate.of(2026, 9, 19)
        )
        assertSoftly(bilde.kjoeringer[1]) {
            // K1, K2, K3, K4 og K6. Ikke koblingen, AVBRUTT eller fakturaen som ikke følger kvartalsregelen
            sendt shouldBe 5
            beloep shouldBeEqualComparingTo BigDecimal("8000")
            feiletNoenGang shouldBe 2
            feilNaa shouldBe 1
            erstattetFoerKjoering shouldBe 1
            ikkeBestilt shouldBe 0
            // K2 og K6; den simulerte meldingen på K4 holdes utenfor (ellers ville d30 vært 1)
            manglendeInnbetaling.antall shouldBe 2
            manglendeInnbetaling.sumUbetalt shouldBeEqualComparingTo BigDecimal("1500")
            manglendeInnbetaling.akkumulert shouldBe AkkumulertDto(d30 = 0, d60 = 1, d90 = 1, d120 = 2, d180 = 2, d365 = 2)
        }
        assertSoftly(bilde.kjoeringer[2]) {
            sendt shouldBe 0
            ikkeBestilt shouldBe 1
        }

        bilde.nesteKjoering.dato shouldBe LocalDate.of(2026, 12, 19)
        bilde.nesteKjoering.antall shouldBe 1
        bilde.nesteKjoering.beloep shouldBeEqualComparingTo BigDecimal("3000")

        assertSoftly(bilde.naa) {
            forfaltIkkeBestilt.antall shouldBe 1
            forfaltIkkeBestilt.eldsteBestillingsdato shouldBe LocalDate.of(2026, 9, 19)
            forfaltIkkeBestilt.eksempler.single().fakturaReferanse shouldBe "FORFALT"
            // Koblingsfakturaen med bestillingsdato fram i tid er ikke sendt
            sendtFoerBestillingsdato.antall shouldBe 1
            sendtFoerBestillingsdato.eksempler.single() shouldBe
                FakturaEksempelDto("SENDT-FOER", serieA, LocalDate.of(2027, 3, 19))
            koblingsfakturaer shouldBe 2
            muligDobbeltfakturering.single().fakturaserieReferanser shouldContainExactlyInAnyOrder listOf(serieA, serieB)
            // Tidligste FEIL-melding på K3, ikke den siste. K1 er ikke i FEIL nå og teller ikke
            feil shouldBe FeilDto(uloest = 2, eldsteFeilDato = LocalDate.of(2026, 6, 25))
            mottak.lytterKjoerer shouldBe true
            mottak.manglendeInnbetalingIkkeVideresendt shouldBe 1
        }
    }

    @Test
    fun `endepunktet svarer Console med UTC-tidspunkter og uten personopplysninger`() {
        lagTestdata()
        jdbc.update("UPDATE ekstern_faktura_status SET opprettet_tidspunkt = ?", LocalDateTime.of(2026, 10, 14, 6, 30, 3))
        // Andre tester rydder ikke faktura_mottak_feil, så tidspunktet må være det siste
        jdbc.update(
            "INSERT INTO faktura_mottak_feil (error, opprettet_tidspunkt) VALUES (?, ?)",
            "Ukjent faktura for $FNR_1", LocalDateTime.of(2099, 6, 21, 12, 16, 11)
        )

        val svar = webClient.get()
            .uri("/admin/statistikk/oyeblikksbilde")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(mapOf("idtyp" to "app")))
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .returnResult().responseBody!!

        svar shouldContain "\"sisteStatusmelding\":\"2026-10-14T06:30:03Z\""
        svar shouldContain "\"mottatt\":\"2026-10-14T06:30:03Z\""
        svar shouldContain "\"sisteMottaksfeil\":\"2099-06-21T12:16:11Z\""
        svar shouldNotContain FNR_1
        svar shouldNotContain FNR_2
        svar shouldNotContain ORGNR
        svar shouldNotContain "123.456.789"
        svar shouldNotContain "Ola Nordmann"
        svar shouldNotContain "78901"
        svar shouldContain "\"feil\":{\"uloest\":2,\"eldsteFeilDato\":\"2026-06-25\"}"
    }

    @Test
    fun `lister videresendte meldinger om manglende innbetaling til avstemming`() {
        val (serieA, _) = lagTestdata()
        val leggTilMelding = { faktura: String, sendt: Boolean?, feilmelding: String? ->
            jdbc.update(
                "INSERT INTO ekstern_faktura_status (faktura_id, dato, status, sendt, feilmelding, opprettet_tidspunkt) " +
                    "SELECT id, '2026-10-02', 'MANGLENDE_INNBETALING', ?, ?, now() FROM faktura WHERE referanse_nr = ?",
                sendt, feilmelding, faktura
            )
        }
        leggTilMelding("K4", true, StatistikkRepository.SIMULERT_MANGLENDE_INNBETALING)
        leggTilMelding("K4", null, null)
        // K6 er mottatt før K2, selv om datoen fra OEBS er senere
        jdbc.update(
            "UPDATE ekstern_faktura_status SET opprettet_tidspunkt = CASE dato WHEN '2026-10-01' THEN ?::timestamp ELSE ?::timestamp END",
            LocalDateTime.of(2026, 8, 1, 6, 30, 3), LocalDateTime.of(2026, 9, 1, 7, 0)
        )

        // Fakturanummeret blir tomt når en senere statusmelding mangler det
        jdbc.update("UPDATE faktura SET eksternt_fakturanummer = '' WHERE referanse_nr = 'K6'")

        statistikkService.hentOyeblikksbilde(NAA).meldingerOmManglendeInnbetaling shouldContainExactly listOf(
            MeldingOmManglendeInnbetalingDto(null, serieA, LocalDate.of(2026, 10, 1), Instant.parse("2026-08-01T06:30:03Z")),
            MeldingOmManglendeInnbetalingDto("EKSTERN-K2", serieA, LocalDate.of(2026, 8, 10), Instant.parse("2026-09-01T07:00:00Z")),
        )

        val svar = webClient.get()
            .uri("/admin/statistikk/oyeblikksbilde")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(mapOf("idtyp" to "app")))
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .returnResult().responseBody!!

        svar shouldContain "\"fakturanummer\":null,\"fakturaserieReferanse\":\"$serieA\",\"datoFraOebs\":\"2026-10-01\""
    }

    @Test
    fun `kvartalsregelen er den samme som FakturaGenerator bruker`() {
        val idag = LocalDate.now()
        val start = idag.withDayOfMonth(1).withMonth(idag.month.firstMonthOfQuarter().value).plusMonths(6)
        webClient.post()
            .uri("/fakturaserier")
            .header("Nav-User-Id", "Z123456")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(mapOf("roles" to "faktureringskomponenten-skriv")))
            .bodyValue(
                AdminControllerIT.lagFakturaserieDto(
                    fakturaseriePeriode = listOf(FakturaseriePeriodeDto.forTest {
                        startDato = start
                        sluttDato = start.plusYears(1).minusDays(1)
                    })
                )
            )
            .exchange()
            .expectStatus().isOk
        val bestillingsdatoer = jdbc.query("SELECT dato_bestilt FROM faktura ORDER BY dato_bestilt") { rs, _ ->
            rs.getObject(1, LocalDate::class.java)
        }
        bestillingsdatoer shouldHaveSize 4

        val dagenFoer = statistikkService.hentOyeblikksbilde(middag(bestillingsdatoer.first().minusDays(1)))
        dagenFoer.nesteKjoering.dato shouldBe bestillingsdatoer.first()
        dagenFoer.nesteKjoering.antall shouldBe 1
        dagenFoer.nesteKjoering.beloep shouldBeEqualComparingTo BigDecimal("3000")

        val etterpaa = statistikkService.hentOyeblikksbilde(middag(bestillingsdatoer.last()))
        etterpaa.kjoeringer.map { it.dato } shouldContainExactly bestillingsdatoer
        etterpaa.kjoeringer.forAll { it.ikkeBestilt shouldBe 1 }
    }

    @Test
    fun `regner ut perioder, planlagt og fakturaserier`() {
        lagTestdataForPerioder()

        val bilde = statistikkService.hentOyeblikksbilde(NAA)

        bilde.perioder.map { "${it.type} ${it.noekkel}" } shouldContainExactly listOf(
            "KVARTAL 2026-K3", "KVARTAL 2026-K2", "KVARTAL 2025-K4",
            "AAR 2026", "AAR 2025",
            "GJELDER_AAR 2026", "GJELDER_AAR 2025", "GJELDER_AAR 2024",
            "ALT alt",
        )
        val periode = bilde.perioder.associateBy { "${it.type} ${it.noekkel}" }
        assertSoftly(periode.getValue("ALT alt")) {
            // Koblingen, AVBRUTT og de planlagte er ikke sendt
            sendt shouldBe 8
            fakturert shouldBeEqualComparingTo BigDecimal("5600")
            kreditert shouldBeEqualComparingTo BigDecimal("-700")
            netto shouldBeEqualComparingTo BigDecimal("4900")
            personer shouldBe 2
            perType.mapValues { (_, v) -> v.antall to v.beloep.toInt() } shouldBe mapOf(
                "VANLIG" to (4L to 4500), "AVREGNING_TILLEGG" to (1L to 300), "AVREGNING_FRADRAG" to (1L to -200),
                "AARSAVREGNING" to (1L to 800), "KREDITERING" to (1L to -500),
            )
            perBestillingsmaate.mapValues { (_, v) -> v.antall to v.beloep.toInt() } shouldBe mapOf(
                "KVARTALSKJOERING" to (2L to 3000), "UTENOM" to (6L to 1900),
            )
            // V1 står i MANGLENDE_INNBETALING bare etter en simulert melding og telles som BESTILT
            perStatus shouldBe mapOf("BESTILT" to 5L, "MANGLENDE_INNBETALING" to 1L, "FEIL" to 1L, "INNE_I_OEBS" to 1L)
            feil shouldBe PeriodeFeilDto(noenGang = 2, loest = 1, uloest = 1)
            // Den simulerte meldingen på V1 er holdt utenfor. Ikke betalt: V2 (ubetalt lik fakturabeløpet),
            // delvis betalt: TILLEGG, SATS (ubetalt 0) og AAR (ubetalt over fakturabeløpet), uten detaljer: FRADRAG
            manglendeInnbetaling.copy(ubetalt = BigDecimal.ZERO) shouldBe PeriodeManglendeInnbetalingDto(
                antall = 5, ikkeBetalt = 1, delvisBetalt = 3, utenDetaljer = 1, ubetalt = BigDecimal.ZERO
            )
            manglendeInnbetaling.ubetalt shouldBeEqualComparingTo BigDecimal("3000")
        }
        assertSoftly(periode.getValue("KVARTAL 2026-K2")) {
            sendt shouldBe 2
            netto shouldBeEqualComparingTo BigDecimal("3000")
            // To fakturaer på samme fødselsnummer
            personer shouldBe 1
        }
        assertSoftly(periode.getValue("KVARTAL 2026-K3")) {
            sendt shouldBe 5
            netto shouldBeEqualComparingTo BigDecimal("1100")
            personer shouldBe 2
        }
        periode.getValue("AAR 2026").sendt shouldBe 7
        periode.getValue("AAR 2025").netto shouldBeEqualComparingTo BigDecimal("800")
        // U1 har linjer i både 2025 og 2026 og telles i begge med linjesummen for året
        assertSoftly(periode.getValue("GJELDER_AAR 2026")) {
            sendt shouldBe 7
            fakturert shouldBeEqualComparingTo BigDecimal("4400")
            kreditert shouldBeEqualComparingTo BigDecimal("-700")
            netto shouldBeEqualComparingTo BigDecimal("3700")
            perType.getValue("VANLIG").beloep shouldBeEqualComparingTo BigDecimal("4100")
        }
        assertSoftly(periode.getValue("GJELDER_AAR 2025")) {
            sendt shouldBe 1
            netto shouldBeEqualComparingTo BigDecimal("400")
            feil.uloest shouldBe 1
        }
        periode.getValue("GJELDER_AAR 2024").perType.getValue("AARSAVREGNING").antall shouldBe 1
        bilde.perioder.forAll {
            with(it.manglendeInnbetaling) { ikkeBetalt + delvisBetalt + utenDetaljer shouldBe antall }
        }

        bilde.utenomPerKvartal.map { it.kvartal } shouldContainExactly listOf("2025-K4", "2026-K3")
        assertSoftly(bilde.utenomPerKvartal[1]) {
            sendt shouldBe 5
            beloep shouldBeEqualComparingTo BigDecimal("1100")
            perType.mapValues { (_, v) -> v.antall } shouldBe mapOf(
                "VANLIG" to 2L, "AVREGNING_TILLEGG" to 1L, "AVREGNING_FRADRAG" to 1L, "AARSAVREGNING" to 0L, "KREDITERING" to 1L
            )
            perKilde shouldBe mapOf("VEDTAK" to 4L, "EOS_PENSJONIST" to 0L, "SATSENDRING" to 1L, "AARSAVREGNING" to 0L, "ANNET" to 0L)
            feiletNoenGang shouldBe 2
            manglendeInnbetaling shouldBe 3
        }
        bilde.utenomPerKvartal[0].perKilde.getValue("AARSAVREGNING") shouldBe 1

        bilde.nesteKjoering.dato shouldBe LocalDate.of(2026, 12, 19)
        assertSoftly(bilde.planlagt) {
            listOf(forfalt, foerNesteKjoering, iNesteKjoering, senere, totalt).map { it.antall to it.beloep.toInt() } shouldBe
                listOf(1L to 100, 1L to 200, 1L to 300, 1L to 400, 4L to 1000)
            sisteBestillingsdato shouldBe LocalDate.of(2027, 3, 19)
            serier shouldBe 2
            personer shouldBe 2
        }
        // Den forfalte fra 2026-K3 er før inneværende kvartal
        bilde.planlagtPerKvartal.map { Triple(it.kvartal, it.antall, it.beloep.toInt()) } shouldContainExactly listOf(
            Triple("2026-K4", 2L, 500), Triple("2027-K1", 1L, 400)
        )

        bilde.fakturaserier shouldBe FakturaserierDto(
            antall = 5,
            personer = 3,
            perKilde = mapOf("VEDTAK" to 1L, "EOS_PENSJONIST" to 1L, "SATSENDRING" to 1L, "AARSAVREGNING" to 1L, "ANNET" to 1L),
            perStatus = mapOf("OPPRETTET" to 1L, "UNDER_BESTILLING" to 1L, "ERSTATTET" to 1L, "KANSELLERT" to 1L, "FERDIG" to 1L),
            perIntervall = mapOf("KVARTAL" to 3L, "SINGEL" to 1L, "MANEDLIG" to 1L),
        )

        val svar = webClient.get()
            .uri("/admin/statistikk/oyeblikksbilde")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(mapOf("idtyp" to "app")))
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .returnResult().responseBody!!
        svar shouldContain "\"perioder\":[{\"type\":\"KVARTAL\",\"noekkel\":\"2026-K3\""
        listOf(FNR_1, FNR_2, FNR_3).forAll { svar shouldNotContain it }
    }

    /**
     * Sendt: V1 og V2 i kvartalskjøringen 19.06.2026, U1 (linjer i 2025 og 2026), KRED, TILLEGG, FRADRAG og SATS
     * utenom i 2026-K3, og AAR (årsavregning) i 2025-K4. Planlagt: én i hver bøtte rundt neste kjøring 19.12.2026.
     */
    private fun lagTestdataForPerioder() {
        val juni = LocalDate.of(2026, 6, 19)
        val juli = LocalDate.of(2026, 7, 1)
        val august = LocalDate.of(2026, 8, 5)
        fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_1
                referanseBruker = "Vedtak om medlemskap datert 01.06.2026"
                status = FakturaserieStatus.UNDER_BESTILLING
                // Simulert i prod: fakturaen settes i MANGLENDE_INNBETALING, men meldingen er simulert
                leggTilFaktura(lagFaktura("V1", FakturaStatus.MANGLENDE_INNBETALING, juni, juli, 1000) {
                    leggTilEksternFakturaStatus(manglende(LocalDate.of(2026, 7, 1), ubetalt = 1000, sendt = true, simulert = true))
                })
                leggTilFaktura(lagFaktura("V2", FakturaStatus.MANGLENDE_INNBETALING, juni, juli, 2000) {
                    leggTilEksternFakturaStatus(manglende(LocalDate.of(2026, 8, 1), ubetalt = 2000, sendt = true, fakturabeloep = 2000))
                })
                leggTilFaktura(lagFaktura("U1", FakturaStatus.FEIL, august, LocalDate.of(2025, 10, 1), 400) {
                    leggTilFakturaLinje(FakturaLinje.forTest {
                        periodeFra = LocalDate.of(2026, 1, 1)
                        periodeTil = LocalDate.of(2026, 3, 31)
                        belop = BigDecimal(600)
                    })
                    leggTilEksternFakturaStatus(feil("Feil", august))
                })
                leggTilFaktura(lagFaktura("KRED", FakturaStatus.INNE_I_OEBS, august, juli, -500) {
                    leggTilEksternFakturaStatus(feil("Feil", august))
                })
                leggTilFaktura(lagFaktura("TILLEGG", FakturaStatus.BESTILT, august, juli, 300) {
                    leggTilEksternFakturaStatus(manglende(LocalDate.of(2026, 9, 1), ubetalt = 100, sendt = true, fakturabeloep = 300))
                })
                leggTilFaktura(lagFaktura("FRADRAG", FakturaStatus.BESTILT, august, juli, -200) {
                    leggTilEksternFakturaStatus(EksternFakturaStatus.forTest {
                        status = FakturaStatus.MANGLENDE_INNBETALING
                        dato = LocalDate.of(2026, 9, 1)
                        fakturaBelop = null
                        ubetaltBelop = null
                    })
                })
                leggTilFaktura(lagFaktura("KOBLING", FakturaStatus.BESTILT, august, juli, 0))
                leggTilFaktura(lagFaktura("AVBRUTT", FakturaStatus.AVBRUTT, juni, juli, 1000))
                leggTilFaktura(lagFaktura("P-FORFALT", FakturaStatus.OPPRETTET, LocalDate.of(2026, 9, 19), LocalDate.of(2026, 10, 1), 100))
                leggTilFaktura(lagFaktura("P-FOER", FakturaStatus.OPPRETTET, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 10, 1), 200))
                leggTilFaktura(lagFaktura("P-NESTE", FakturaStatus.OPPRETTET, LocalDate.of(2026, 12, 19), LocalDate.of(2027, 1, 1), 300))
            }
        )
        fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_1
                referanseBruker = "Årsavregning for 2024"
                fakturaGjelderInnbetalingstype = Innbetalingstype.AARSAVREGNING
                intervall = FakturaserieIntervall.SINGEL
                status = FakturaserieStatus.FERDIG
                leggTilFaktura(lagFaktura("AAR", FakturaStatus.BESTILT, LocalDate.of(2025, 11, 10), LocalDate.of(2024, 1, 1), 800) {
                    leggTilEksternFakturaStatus(manglende(LocalDate.of(2026, 1, 5), ubetalt = 900, sendt = true, fakturabeloep = 800))
                })
            }
        )
        fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_2
                referanseBruker = "Faktura for årlig satsoppdatering 2026"
                status = FakturaserieStatus.ERSTATTET
                leggTilFaktura(lagFaktura("SATS", FakturaStatus.BESTILT, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1), 500) {
                    leggTilEksternFakturaStatus(manglende(LocalDate.of(2026, 10, 1), ubetalt = 0, sendt = true, fakturabeloep = 500))
                })
                leggTilFaktura(lagFaktura("P-SENERE", FakturaStatus.OPPRETTET, LocalDate.of(2027, 3, 19), LocalDate.of(2027, 4, 1), 400))
            }
        )
        fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_2
                referanseBruker = "Informasjon om trygdeavgift"
                status = FakturaserieStatus.KANSELLERT
            }
        )
        fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_3
                referanseBruker = "Noe helt annet"
                intervall = FakturaserieIntervall.MANEDLIG
                status = FakturaserieStatus.OPPRETTET
            }
        )
        jdbc.update(
            "UPDATE faktura SET avregning_faktura_id = (SELECT id FROM faktura WHERE referanse_nr = 'V1') " +
                "WHERE referanse_nr IN ('TILLEGG', 'FRADRAG', 'KOBLING')"
        )
    }

    /** Returnerer referansene til to overlappende serier for samme person. */
    private fun lagTestdata(): Pair<String, String> {
        val juni = LocalDate.of(2026, 6, 19)
        val juli = LocalDate.of(2026, 7, 1)
        val serieA = fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_1
                startdato = LocalDate.of(2026, 1, 1)
                sluttdato = LocalDate.of(2027, 12, 31)
                medFullmektig(organisasjonsnummer = ORGNR)
                leggTilFaktura(lagFaktura("K5", FakturaStatus.INNE_I_OEBS, LocalDate.of(2026, 3, 19), LocalDate.of(2026, 4, 1), 1500))
                leggTilFaktura(lagFaktura("K1", FakturaStatus.INNE_I_OEBS, juni, juli, 3000) {
                    leggTilEksternFakturaStatus(feil("Midlertidig feil", LocalDate.of(2026, 6, 20)))
                })
                leggTilFaktura(lagFaktura("K2", FakturaStatus.MANGLENDE_INNBETALING, juni, juli, 2000) {
                    leggTilEksternFakturaStatus(manglende(LocalDate.of(2026, 8, 10), ubetalt = 2000, sendt = true))
                    leggTilEksternFakturaStatus(manglende(LocalDate.of(2026, 9, 1), ubetalt = 500, sendt = false))
                })
                leggTilFaktura(lagFaktura("K3", FakturaStatus.FEIL, juni, juli, 1000) {
                    leggTilEksternFakturaStatus(feil("Fant ikke kunde $FNR_1 i kundedata", LocalDate.of(2026, 6, 25)))
                    leggTilEksternFakturaStatus(feil("Ola Nordmann 123456-78901", LocalDate.of(2026, 7, 2)))
                })
                leggTilFaktura(lagFaktura("K4", FakturaStatus.MANGLENDE_INNBETALING, juni, juli, 1000) {
                    leggTilEksternFakturaStatus(manglende(LocalDate.of(2026, 6, 25), ubetalt = 1000, sendt = false, simulert = true))
                })
                leggTilFaktura(lagFaktura("K6", FakturaStatus.MANGLENDE_INNBETALING, juni, juli, 1000) {
                    leggTilEksternFakturaStatus(manglende(LocalDate.of(2026, 10, 1), ubetalt = 1000, sendt = true))
                })
                leggTilFaktura(lagFaktura("ERSTATTET", FakturaStatus.AVBRUTT, juni, juli, 1000))
                leggTilFaktura(lagFaktura("KOBLING", FakturaStatus.BESTILT, juni, juli, 0))
                // Samme bestillingsdato, men perioden starter ikke i kvartalet etter
                leggTilFaktura(lagFaktura("UTENOM", FakturaStatus.FEIL, juni, LocalDate.of(2026, 4, 1), 1000) {
                    leggTilEksternFakturaStatus(feil("Fant ikke kunde 123.456.789 01 i kundedata", LocalDate.of(2026, 6, 30)))
                })
                leggTilFaktura(lagFaktura("FORFALT", FakturaStatus.OPPRETTET, LocalDate.of(2026, 9, 19), LocalDate.of(2026, 10, 1), 3000))
                leggTilFaktura(lagFaktura("PLANLAGT", FakturaStatus.OPPRETTET, LocalDate.of(2026, 12, 19), LocalDate.of(2027, 1, 1), 3000))
                leggTilFaktura(lagFaktura("SENDT-FOER", FakturaStatus.BESTILT, LocalDate.of(2027, 3, 19), LocalDate.of(2027, 4, 1), 3000))
                leggTilFaktura(lagFaktura("KOBLING-FRAMTID", FakturaStatus.BESTILT, LocalDate.of(2027, 3, 19), LocalDate.of(2027, 4, 1), 0))
            }
        ).referanse
        val serieB = fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_1
                startdato = LocalDate.of(2027, 1, 1)
                sluttdato = LocalDate.of(2027, 6, 30)
                leggTilFaktura(lagFaktura("B", FakturaStatus.OPPRETTET, LocalDate.of(2027, 3, 19), LocalDate.of(2027, 4, 1), 3000))
            }
        ).referanse
        fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_2
                startdato = LocalDate.of(2026, 1, 1)
                sluttdato = LocalDate.of(2027, 12, 31)
                leggTilFaktura(lagFaktura("C", FakturaStatus.OPPRETTET, LocalDate.of(2027, 6, 19), LocalDate.of(2027, 7, 1), 3000))
            }
        )
        // Avregningskoblingen kan ikke settes via testfabrikken uten at begge fakturaene er lagret
        jdbc.update(
            "UPDATE faktura SET avregning_faktura_id = (SELECT id FROM faktura WHERE referanse_nr = 'K1') " +
                "WHERE referanse_nr IN ('KOBLING', 'KOBLING-FRAMTID')"
        )
        return serieA to serieB
    }

    private fun lagFaktura(
        referanse: String,
        status: FakturaStatus,
        datoBestilt: LocalDate,
        periodeFra: LocalDate,
        beloep: Int,
        init: FakturaTestFactory.Builder.() -> Unit = {},
    ): Faktura = Faktura.forTest {
        referanseNr = referanse
        eksternFakturaNummer = "EKSTERN-$referanse"
        this.status = status
        this.datoBestilt = datoBestilt
        leggTilFakturaLinje(FakturaLinje.forTest {
            this.periodeFra = periodeFra
            periodeTil = periodeFra.plusMonths(3).minusDays(1)
            belop = BigDecimal(beloep)
        })
        init()
    }

    private fun feil(melding: String, dato: LocalDate) = EksternFakturaStatus.forTest {
        status = FakturaStatus.FEIL
        this.dato = dato
        feilMelding = melding
    }

    private fun manglende(dato: LocalDate, ubetalt: Int, sendt: Boolean, simulert: Boolean = false, fakturabeloep: Int = 1000) =
        EksternFakturaStatus.forTest {
            status = FakturaStatus.MANGLENDE_INNBETALING
            this.dato = dato
            this.ubetalt = ubetalt
            beløp = fakturabeloep
            this.sendt = sendt
            if (simulert) feilMelding = "Simulert manglende innbetaling"
        }

    private fun middag(dato: LocalDate): Instant = dato.atTime(12, 0).toInstant(ZoneOffset.UTC)

    private fun token(claims: Map<String, Any>): String =
        server.issueToken(
            "aad",
            consoleKlientId,
            DefaultOAuth2TokenCallback(
                issuerId = "aad",
                subject = consoleKlientId,
                audience = listOf("faktureringskomponenten-localhost"),
                claims = claims,
            )
        ).serialize()

    companion object {
        private val NAA = Instant.parse("2026-10-23T08:00:00Z")
        private const val FNR_1 = "11111111111"
        private const val FNR_2 = "22222222222"
        private const val FNR_3 = "33333333333"
        private const val ORGNR = "987654321"
    }
}
