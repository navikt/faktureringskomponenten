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
import no.nav.faktureringskomponenten.domain.models.FakturaTestFactory
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
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
            // To meldinger som bare skiller seg i tallrekken, grupperes til én
            feil.uloest shouldBe 2
            feil.eldsteFeilDato shouldBe LocalDate.of(2026, 6, 25)
            feil.perFeilmelding shouldContainExactly listOf(
                FeilmeldingDto("Fant ikke kunde *** i kundedata", 2, LocalDate.of(2026, 6, 25))
            )
            mottak.lytterKjoerer shouldBe true
            mottak.manglendeInnbetalingIkkeVideresendt shouldBe 1
        }
    }

    @Test
    fun `endepunktet svarer Console med UTC-tidspunkter og uten personopplysninger`() {
        lagTestdata()
        jdbc.update("UPDATE ekstern_faktura_status SET opprettet_tidspunkt = ?", LocalDateTime.of(2026, 10, 14, 6, 30, 3))
        jdbc.update(
            "UPDATE ekstern_faktura_status SET feilmelding = ? WHERE feilmelding LIKE 'Fant ikke kunde 123.%'",
            "Ukjent 123456-78901, 1111/1111/111, 1111_1111_111 og 1111\u00A01111\u00A0111"
        )
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
        svar shouldContain "\"sisteMottaksfeil\":\"2099-06-21T12:16:11Z\""
        svar shouldNotContain FNR_1
        svar shouldNotContain FNR_2
        svar shouldNotContain ORGNR
        svar shouldNotContain "123.456.789"
        svar shouldContain "Ukjent ***, ***, *** og ***"
        listOf("123456", "78901", "1111").forEach { svar shouldNotContain it }
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

    private fun manglende(dato: LocalDate, ubetalt: Int, sendt: Boolean, simulert: Boolean = false) =
        EksternFakturaStatus.forTest {
            status = FakturaStatus.MANGLENDE_INNBETALING
            this.dato = dato
            this.ubetalt = ubetalt
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
        private const val ORGNR = "987654321"
    }
}
