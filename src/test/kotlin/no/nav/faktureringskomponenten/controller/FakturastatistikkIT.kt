package no.nav.faktureringskomponenten.controller

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.faktureringskomponenten.domain.models.EksternFakturaStatus
import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaLinje
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.domain.models.FakturaTestFactory
import no.nav.faktureringskomponenten.domain.models.Fakturaserie
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import no.nav.faktureringskomponenten.domain.models.FakturaserieStatus
import no.nav.faktureringskomponenten.domain.models.Innbetalingstype
import no.nav.faktureringskomponenten.domain.models.forTest
import no.nav.faktureringskomponenten.domain.repositories.FakturaserieRepository
import no.nav.faktureringskomponenten.service.integration.kafka.EmbeddedKafkaBase
import no.nav.faktureringskomponenten.service.statistikk.Fakturakilde
import no.nav.faktureringskomponenten.service.statistikk.Fakturaseriehendelse
import no.nav.faktureringskomponenten.service.statistikk.FakturastatistikkService
import no.nav.faktureringskomponenten.service.statistikk.Fakturatype
import no.nav.faktureringskomponenten.service.statistikk.Fullmektigtype
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
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Testcontainers
@ActiveProfiles("itest", "embeded-kafka")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableMockOAuth2Server
class FakturastatistikkIT(
    @param:Autowired private val webClient: WebTestClient,
    @param:Autowired private val server: MockOAuth2Server,
    @param:Autowired private val fakturaserieRepository: FakturaserieRepository,
    @param:Autowired private val jdbc: JdbcTemplate,
    @param:Autowired private val fakturastatistikkService: FakturastatistikkService,
    @param:Value("\${admin.console-klient-id}") private val consoleKlientId: String,
) : EmbeddedKafkaBase(fakturaserieRepository) {

    private val idag = LocalDate.now(ZoneId.of("Europe/Oslo"))

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
    fun `tom database gir nuller og fulle fordelinger`() {
        val statistikk = fakturastatistikkService.hentStatistikk(null, null)

        assertSoftly(statistikk) {
            bestilt.antall shouldBe 0
            bestilt.perStatus.keys shouldContainExactlyInAnyOrder FakturastatistikkService.SENDTE_STATUSER
            bestilt.perType.values.sum() shouldBe 0
            fakturaserier.antall shouldBe 0
            fakturaserier.perKilde.keys shouldContainExactlyInAnyOrder Fakturakilde.entries
            drift.forfaltIkkeBestilt.antall shouldBe 0
            drift.venterPaaKvittering.antall shouldBe 0
            drift.muligDobbeltfakturering shouldHaveSize 0
            perMaaned shouldHaveSize 24
            perMaaned[11].maaned shouldBe idag.toString().substring(0, 7)
        }
    }

    @Test
    fun `regner ut drift, bestilte, planlagte og fakturaserier`() {
        val (serieA, serieB) = lagTestdata()

        val statistikk = fakturastatistikkService.hentStatistikk(null, null)

        assertSoftly(statistikk.drift) {
            forfaltIkkeBestilt.antall shouldBe 1
            forfaltIkkeBestilt.eldsteBestillingsdato shouldBe idag.minusDays(2)
            forfaltIkkeBestilt.eldste.map { it.fakturaserieReferanse } shouldContainExactly listOf(serieA)

            venterPaaKvittering.antall shouldBe 2
            venterPaaKvittering.under3Dager shouldBe 1
            venterPaaKvittering.mellom7Og30Dager shouldBe 1
            venterPaaKvittering.eldsteOver7Dager.map { it.fakturaReferanse } shouldContainExactly listOf("VENTER-GAMMEL")
            venterPaaKvittering.antallKoblingsfakturaerHoldtUtenfor shouldBe 1

            feil.antall shouldBe 1
            feil.perFeilmelding.single().melding shouldBe "Ugyldig kundenummer"
            feil.eldsteFeilDato shouldBe idag.minusDays(19)

            antallManglendeInnbetaling shouldBe 2
            muligDobbeltfakturering.single().fakturaserieReferanser shouldContainExactlyInAnyOrder listOf(serieA, serieB)
        }

        assertSoftly(statistikk.bestilt) {
            // INNE_I_OEBS x3 (ordinær, avregning, kreditering) + årsavregning, 2 manglende, 1 feil, 2 ventende
            antall shouldBe 9
            perStatus[FakturaStatus.INNE_I_OEBS] shouldBe 4
            perStatus[FakturaStatus.MANGLENDE_INNBETALING] shouldBe 2
            perStatus[FakturaStatus.FEIL] shouldBe 1
            perStatus[FakturaStatus.BESTILT] shouldBe 2
            perType[Fakturatype.AVREGNING] shouldBe 1
            perType[Fakturatype.KREDITERING] shouldBe 1
            perType[Fakturatype.ORDINAER] shouldBe 7
            perInnbetalingstype[Innbetalingstype.AARSAVREGNING] shouldBe 1
            sumFakturert shouldBeEqualComparingTo BigDecimal("20500")
            sumKreditert shouldBeEqualComparingTo BigDecimal("-1000")
            sumNetto shouldBeEqualComparingTo BigDecimal("19500")
            antallUnikePersoner shouldBe 2
            manglendeInnbetaling.ikkeBetalt shouldBe 1
            manglendeInnbetaling.delvisBetalt shouldBe 1
            manglendeInnbetaling.utenDetaljer shouldBe 0
            manglendeInnbetaling.sumUbetalt shouldBeEqualComparingTo BigDecimal("4000")
        }

        assertSoftly(statistikk.planlagt) {
            antall shouldBe 4
            neste30Dager shouldBe 2
            antallFakturaserier shouldBe 2
            antallUnikePersoner shouldBe 1
        }

        assertSoftly(statistikk.fakturaserier) {
            antall shouldBe 4
            antallUnikePersoner shouldBe 2
            perKilde[Fakturakilde.VEDTAK] shouldBe 1
            perKilde[Fakturakilde.SATSENDRING] shouldBe 1
            perKilde[Fakturakilde.AARSAVREGNING] shouldBe 1
            perKilde[Fakturakilde.ANNET] shouldBe 1
            perHendelse[Fakturaseriehendelse.ENDRING] shouldBe 1
            perHendelse[Fakturaseriehendelse.NY] shouldBe 3
            perFullmektig[Fullmektigtype.ORGANISASJON] shouldBe 1
            perFullmektig[Fullmektigtype.INGEN] shouldBe 3
            perStatus[FakturaserieStatus.ERSTATTET] shouldBe 1
            perIntervall[FakturaserieIntervall.SINGEL] shouldBe 1
        }
    }

    @Test
    fun `periodefilteret avgrenser bestilte på bestillingsdato, men ikke nåtilstanden`() {
        lagTestdata()

        val statistikk = fakturastatistikkService.hentStatistikk(idag.minusDays(25), idag)

        // Feil (-20), ventende (-10, -1), årsavregning (-5) – ikke de eldre
        statistikk.bestilt.antall shouldBe 4
        statistikk.drift.forfaltIkkeBestilt.antall shouldBe 1
        statistikk.planlagt.antall shouldBe 4
    }

    @Test
    fun `endepunktet svarer Console uten personopplysninger`() {
        lagTestdata()

        val svar = webClient.get()
            .uri("/admin/statistikk?fraOgMed=${idag.minusYears(1)}&tilOgMed=$idag")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + maskinToken())
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .returnResult().responseBody!!

        svar shouldContain "\"venterPaaKvittering\""
        svar shouldNotContain FNR_1
        svar shouldNotContain FNR_2
        svar shouldNotContain ORGNR
    }

    @Test
    fun `tallrekker i feilmeldinger fra OEBS maskeres og grupperes sammen`() {
        fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                listOf("123456 78901", "98765432109").forEachIndexed { i, fnr ->
                    leggTilFaktura(
                        lagFaktura("FEIL-$i", FakturaStatus.FEIL, idag.minusDays(3), 1000) {
                            leggTilEksternFakturaStatus(EksternFakturaStatus.forTest {
                                status = FakturaStatus.FEIL
                                dato = idag.minusDays(2)
                                feilMelding = "Fant ikke kunde $fnr i kundedata, faktura 99991234"
                            })
                        }
                    )
                }
            }
        )

        val feilmelding = fakturastatistikkService.hentStatistikk(null, null).drift.feil.perFeilmelding.single()

        feilmelding.melding shouldBe "Fant ikke kunde *** i kundedata, faktura ***"
        feilmelding.antall shouldBe 2
    }

    @Test
    fun `endepunktet avviser fraOgMed etter tilOgMed`() {
        webClient.get()
            .uri("/admin/statistikk?fraOgMed=$idag&tilOgMed=${idag.minusDays(1)}")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + maskinToken())
            .exchange()
            .expectStatus().isBadRequest
    }

    /** Returnerer referansene til de to overlappende seriene for samme person. */
    private fun lagTestdata(): Pair<String, String> {
        val serieA = fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_1
                referanseBruker = "Vedtak om medlemskap datert 01.01.2026"
                startdato = idag.minusMonths(6)
                sluttdato = idag.plusMonths(6)
                medFullmektig(organisasjonsnummer = ORGNR)
                leggTilFaktura(lagFaktura("ORDINAER-OK", FakturaStatus.INNE_I_OEBS, idag.minusDays(60), 3000))
                leggTilFaktura(
                    lagFaktura("MANGLENDE-HEL", FakturaStatus.MANGLENDE_INNBETALING, idag.minusDays(40), 3000) {
                        leggTilEksternFakturaStatus(manglende(fakturert = 3000, utestaaende = 3000))
                    }
                )
                leggTilFaktura(
                    lagFaktura("MANGLENDE-DELVIS", FakturaStatus.MANGLENDE_INNBETALING, idag.minusDays(30), 3000) {
                        leggTilEksternFakturaStatus(manglende(fakturert = 3000, utestaaende = 1000))
                    }
                )
                leggTilFaktura(
                    lagFaktura("FEILET", FakturaStatus.FEIL, idag.minusDays(20), 3000) {
                        leggTilEksternFakturaStatus(EksternFakturaStatus.forTest {
                            status = FakturaStatus.FEIL
                            dato = idag.minusDays(19)
                            feilMelding = "Ugyldig kundenummer"
                        })
                    }
                )
                leggTilFaktura(lagFaktura("VENTER-GAMMEL", FakturaStatus.BESTILT, idag.minusDays(10), 3000))
                leggTilFaktura(lagFaktura("VENTER-NY", FakturaStatus.BESTILT, idag.minusDays(1), 3000))
                leggTilFaktura(lagFaktura("FORFALT", FakturaStatus.OPPRETTET, idag.minusDays(2), 3000))
                leggTilFaktura(lagFaktura("PLANLAGT-SNART", FakturaStatus.OPPRETTET, idag.plusDays(10), 3000))
                leggTilFaktura(lagFaktura("PLANLAGT-SENERE", FakturaStatus.OPPRETTET, idag.plusDays(60), 3000))
                leggTilFaktura(lagFaktura("AVREGNING", FakturaStatus.INNE_I_OEBS, idag.minusDays(50), 500))
                leggTilFaktura(lagFaktura("KOBLING", FakturaStatus.BESTILT, idag.minusDays(50), 0))
                leggTilFaktura(lagFaktura("KREDITERING", FakturaStatus.INNE_I_OEBS, idag.minusDays(45), -1000))
            }
        ).referanse

        val serieB = fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_1
                referanseBruker = "Faktura for årlig satsoppdatering av trygdeavgift"
                startdato = idag
                sluttdato = idag.plusMonths(3)
                leggTilFaktura(lagFaktura("SATSENDRING-PLANLAGT", FakturaStatus.OPPRETTET, idag.plusDays(20), 1000))
            }
        )

        // Serien som ble erstattet av serie B, så B regnes som en endring
        fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_1
                status = FakturaserieStatus.ERSTATTET
                erstattetMed = serieB
            }
        )

        fakturaserieRepository.saveAndFlush(
            Fakturaserie.forTest {
                referanse = ULID.randomULID()
                fodselsnummer = FNR_2
                referanseBruker = "Årsavregning datert 01.03.2026"
                fakturaGjelderInnbetalingstype = Innbetalingstype.AARSAVREGNING
                intervall = FakturaserieIntervall.SINGEL
                leggTilFaktura(lagFaktura("AARSAVREGNING", FakturaStatus.INNE_I_OEBS, idag.minusDays(5), 2000))
            }
        )

        // Avregningskoblingen kan ikke settes via testfabrikken uten at begge fakturaene er lagret
        jdbc.update(
            "UPDATE faktura SET avregning_faktura_id = (SELECT id FROM faktura WHERE referanse_nr = 'ORDINAER-OK') " +
                "WHERE referanse_nr IN ('AVREGNING', 'KOBLING')"
        )
        // Endret-tidspunktet settes av auditeringen ved lagring (UTC); flytt det tilbake for den gamle ventende
        jdbc.update(
            "UPDATE faktura SET endret_tidspunkt = ? WHERE referanse_nr = 'VENTER-GAMMEL'",
            LocalDateTime.ofInstant(Instant.now().minus(10, ChronoUnit.DAYS), ZoneOffset.UTC)
        )

        return serieA to serieB.referanse
    }

    private fun lagFaktura(
        referanse: String,
        status: FakturaStatus,
        datoBestilt: LocalDate,
        beloep: Int,
        init: FakturaTestFactory.Builder.() -> Unit = {},
    ): Faktura = Faktura.forTest {
        referanseNr = referanse
        eksternFakturaNummer = "EKSTERN-$referanse"
        this.status = status
        this.datoBestilt = datoBestilt
        leggTilFakturaLinje(FakturaLinje.forTest { belop = BigDecimal(beloep) })
        init()
    }

    private fun manglende(fakturert: Int, utestaaende: Int) = EksternFakturaStatus.forTest {
        status = FakturaStatus.MANGLENDE_INNBETALING
        dato = idag.minusDays(1)
        beløp = fakturert
        ubetalt = utestaaende
        sendt = true
    }

    private fun maskinToken(): String =
        server.issueToken(
            "aad",
            consoleKlientId,
            DefaultOAuth2TokenCallback(
                issuerId = "aad",
                subject = consoleKlientId,
                audience = listOf("faktureringskomponenten-localhost"),
                claims = mapOf("idtyp" to "app"),
            )
        ).serialize()

    companion object {
        private const val FNR_1 = "11111111111"
        private const val FNR_2 = "22222222222"
        private const val ORGNR = "987654321"
    }
}
