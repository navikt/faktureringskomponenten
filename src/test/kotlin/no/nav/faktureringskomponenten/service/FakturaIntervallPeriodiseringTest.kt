package no.nav.faktureringskomponenten.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.FakturaserieIntervall
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("FakturaIntervallPeriodiseringTest")
class FakturaIntervallPeriodiseringTest {

    @Nested
    @DisplayName("genererPeriodisering for intervall MÅNEDLIG")
    inner class MånedligIntervallTest {
        @Test
        fun `skal generere korrekte perioder for én måned`() {
            val startDato = LocalDate.of(2024, 1, 1)
            val sluttDato = LocalDate.of(2024, 1, 31)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.MANEDLIG
            )

            perioder shouldHaveSize 1
            perioder.first() shouldBe (startDato to sluttDato)
        }

        @Test
        fun `skal generere korrekte perioder for tre måneder`() {
            val startDato = LocalDate.of(2024, 1, 1)
            val sluttDato = LocalDate.of(2024, 3, 31)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.MANEDLIG
            )

            perioder shouldHaveSize 3
            perioder[0] shouldBe (LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 1, 31))
            perioder[1] shouldBe (LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 2, 29))
            perioder[2] shouldBe (LocalDate.of(2024, 3, 1) to LocalDate.of(2024, 3, 31))
        }

        @Test
        fun `skal håndtere skuddår korrekt`() {
            val startDato = LocalDate.of(2024, 2, 1)
            val sluttDato = LocalDate.of(2024, 2, 29)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.MANEDLIG
            )

            perioder shouldHaveSize 1
            perioder.first() shouldBe (startDato to sluttDato)
        }

        @Test
        fun `skal generere korrekte perioder over årsskifte`() {
            val startDato = LocalDate.of(2024, 12, 1)
            val sluttDato = LocalDate.of(2025, 2, 28)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.MANEDLIG
            )

            perioder shouldHaveSize 3
            perioder[0] shouldBe (LocalDate.of(2024, 12, 1) to LocalDate.of(2024, 12, 31))
            perioder[1] shouldBe (LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 1, 31))
            perioder[2] shouldBe (LocalDate.of(2025, 2, 1) to LocalDate.of(2025, 2, 28))
        }

        @Test
        fun `skal generere korrekte perioder for et helt år med delperioder i start og slutt`() {
            val startDato = LocalDate.of(2024, 3, 15)
            val sluttDato = LocalDate.of(2025, 2, 15)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.MANEDLIG
            )

            perioder shouldHaveSize 12
            perioder[0] shouldBe (LocalDate.of(2024, 3, 15) to LocalDate.of(2024, 3, 31))
            perioder[1] shouldBe (LocalDate.of(2024, 4, 1) to LocalDate.of(2024, 4, 30))
            perioder[2] shouldBe (LocalDate.of(2024, 5, 1) to LocalDate.of(2024, 5, 31))
            perioder[3] shouldBe (LocalDate.of(2024, 6, 1) to LocalDate.of(2024, 6, 30))
            perioder[4] shouldBe (LocalDate.of(2024, 7, 1) to LocalDate.of(2024, 7, 31))
            perioder[5] shouldBe (LocalDate.of(2024, 8, 1) to LocalDate.of(2024, 8, 31))
            perioder[6] shouldBe (LocalDate.of(2024, 9, 1) to LocalDate.of(2024, 9, 30))
            perioder[7] shouldBe (LocalDate.of(2024, 10, 1) to LocalDate.of(2024, 10, 31))
            perioder[8] shouldBe (LocalDate.of(2024, 11, 1) to LocalDate.of(2024, 11, 30))
            perioder[9] shouldBe (LocalDate.of(2024, 12, 1) to LocalDate.of(2024, 12, 31))
            perioder[10] shouldBe (LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 1, 31))
            perioder[11] shouldBe (LocalDate.of(2025, 2, 1) to LocalDate.of(2025, 2, 15))
        }

        @Test
        fun `skal håndtere skuddår og ikke-skuddår over flere år`() {
            val startDato = LocalDate.of(2024, 2, 1)  // skuddår
            val sluttDato = LocalDate.of(2025, 2, 28) // ikke skuddår

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.MANEDLIG
            )

            perioder shouldHaveSize 13
            perioder[0] shouldBe (LocalDate.of(2024, 2, 1) to LocalDate.of(2024, 2, 29))  // skuddår februar
            perioder[12] shouldBe (LocalDate.of(2025, 2, 1) to LocalDate.of(2025, 2, 28)) // normal februar
        }
    }

    @Nested
    @DisplayName("genererPeriodisering for intervall KVARTAL")
    inner class KvartalvisIntervallTest {

        @Test
        fun `skal generere korrekte perioder for ett kvartal`() {
            val startDato = LocalDate.of(2024, 1, 1)
            val sluttDato = LocalDate.of(2024, 3, 31)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.KVARTAL
            )

            perioder shouldHaveSize 1
            perioder.first() shouldBe (startDato to sluttDato)
        }

        @Test
        fun `skal generere korrekte perioder for helt år`() {
            val startDato = LocalDate.of(2024, 1, 1)
            val sluttDato = LocalDate.of(2024, 12, 31)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.KVARTAL
            )

            perioder shouldHaveSize 4
            perioder[0] shouldBe (LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 3, 31))
            perioder[1] shouldBe (LocalDate.of(2024, 4, 1) to LocalDate.of(2024, 6, 30))
            perioder[2] shouldBe (LocalDate.of(2024, 7, 1) to LocalDate.of(2024, 9, 30))
            perioder[3] shouldBe (LocalDate.of(2024, 10, 1) to LocalDate.of(2024, 12, 31))
        }

        @Test
        fun `skal håndtere delperioder av kvartal`() {
            val startDato = LocalDate.of(2024, 2, 15)
            val sluttDato = LocalDate.of(2024, 5, 15)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.KVARTAL
            )

            perioder shouldHaveSize 2
            perioder[0] shouldBe (LocalDate.of(2024, 2, 15) to LocalDate.of(2024, 3, 31))
            perioder[1] shouldBe (LocalDate.of(2024, 4, 1) to LocalDate.of(2024, 5, 15))
        }

        @Test
        fun `skal generere korrekte perioder over to år`() {
            val startDato = LocalDate.of(2024, 10, 1)
            val sluttDato = LocalDate.of(2025, 3, 31)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.KVARTAL
            )

            perioder shouldHaveSize 2
            perioder[0] shouldBe (LocalDate.of(2024, 10, 1) to LocalDate.of(2024, 12, 31))
            perioder[1] shouldBe (LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 3, 31))
        }

        @Test
        fun `skal generere korrekte perioder for tre år`() {
            val startDato = LocalDate.of(2024, 1, 1)
            val sluttDato = LocalDate.of(2026, 12, 31)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.KVARTAL
            )

            perioder shouldHaveSize 12
            // 2024
            perioder[0] shouldBe (LocalDate.of(2024, 1, 1) to LocalDate.of(2024, 3, 31))
            perioder[1] shouldBe (LocalDate.of(2024, 4, 1) to LocalDate.of(2024, 6, 30))
            perioder[2] shouldBe (LocalDate.of(2024, 7, 1) to LocalDate.of(2024, 9, 30))
            perioder[3] shouldBe (LocalDate.of(2024, 10, 1) to LocalDate.of(2024, 12, 31))
            // 2025
            perioder[4] shouldBe (LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 3, 31))
            perioder[5] shouldBe (LocalDate.of(2025, 4, 1) to LocalDate.of(2025, 6, 30))
            perioder[6] shouldBe (LocalDate.of(2025, 7, 1) to LocalDate.of(2025, 9, 30))
            perioder[7] shouldBe (LocalDate.of(2025, 10, 1) to LocalDate.of(2025, 12, 31))
            // 2026
            perioder[8] shouldBe (LocalDate.of(2026, 1, 1) to LocalDate.of(2026, 3, 31))
            perioder[9] shouldBe (LocalDate.of(2026, 4, 1) to LocalDate.of(2026, 6, 30))
            perioder[10] shouldBe (LocalDate.of(2026, 7, 1) to LocalDate.of(2026, 9, 30))
            perioder[11] shouldBe (LocalDate.of(2026, 10, 1) to LocalDate.of(2026, 12, 31))
        }

        @Test
        fun `skal håndtere delperiode i start og slutt over flere år`() {
            val startDato = LocalDate.of(2024, 11, 15)
            val sluttDato = LocalDate.of(2026, 2, 15)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.KVARTAL
            )

            perioder shouldHaveSize 6
            perioder[0] shouldBe (LocalDate.of(2024, 11, 15) to LocalDate.of(2024, 12, 31))
            perioder[1] shouldBe (LocalDate.of(2025, 1, 1) to LocalDate.of(2025, 3, 31))
            perioder[2] shouldBe (LocalDate.of(2025, 4, 1) to LocalDate.of(2025, 6, 30))
            perioder[3] shouldBe (LocalDate.of(2025, 7, 1) to LocalDate.of(2025, 9, 30))
            perioder[4] shouldBe (LocalDate.of(2025, 10, 1) to LocalDate.of(2025, 12, 31))
            perioder[5] shouldBe (LocalDate.of(2026, 1, 1) to LocalDate.of(2026, 2, 15))
        }
    }

    @Nested
    @DisplayName("genererPeriodisering for intervall SINGEL")
    inner class SingelIntervallTest {

        @Test
        fun `skal kaste IllegalArgumentException`() {
            val startDato = LocalDate.of(2024, 1, 1)
            val sluttDato = LocalDate.of(2024, 12, 31)

            shouldThrow<IllegalArgumentException> {
                FakturaIntervallPeriodisering.genererPeriodisering(
                    startDato,
                    sluttDato,
                    FakturaserieIntervall.SINGEL
                )
            }
        }
    }

    @Nested
    @DisplayName("edge cases")
    inner class EdgeCasesTest {

        @Test
        fun `skal håndtere når start- og sluttdato er samme dag`() {
            val dato = LocalDate.of(2024, 1, 1)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                dato,
                dato,
                FakturaserieIntervall.MANEDLIG
            )

            perioder shouldHaveSize 1
            perioder.first() shouldBe (dato to dato)
        }

        @Test
        fun `skal returnere tom liste når startdato er etter sluttdato`() {
            val startDato = LocalDate.of(2024, 2, 1)
            val sluttDato = LocalDate.of(2024, 1, 1)

            val perioder = FakturaIntervallPeriodisering.genererPeriodisering(
                startDato,
                sluttDato,
                FakturaserieIntervall.MANEDLIG
            )

            perioder shouldHaveSize 0
        }
    }
}
