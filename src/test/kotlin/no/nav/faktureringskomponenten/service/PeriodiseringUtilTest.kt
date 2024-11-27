package no.nav.faktureringskomponenten.service

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.service.PeriodiseringUtil.substract
import org.junit.jupiter.api.Test
import org.threeten.extra.LocalDateRange
import java.time.LocalDate

class PeriodiseringUtilTest {

    @Test
    fun `substract a period`() {
        val substracted = LocalDateRange.of(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 10)
        ).substract(
            LocalDateRange.of(
                LocalDate.of(2024, 1, 3),
                LocalDate.of(2024, 2, 12)
            )
        )

        substracted shouldBe listOf(
            LocalDateRange.of(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 2)
            )
        )
    }


    @Test
    fun `substract a period in the middle`() {
        val substracted = LocalDateRange.of(
            LocalDate.of(2023, 12, 16),
            LocalDate.of(2024, 1, 15)
        ).substract(
            LocalDateRange.of(
                LocalDate.of(2023, 12, 31),
                LocalDate.of(2024, 1, 13)
            )
        )

        substracted.shouldContainExactlyInAnyOrder(
            LocalDateRange.of(LocalDate.of(2023, 12, 16), LocalDate.of(2023, 12, 30)),
            LocalDateRange.of(LocalDate.of(2024, 1, 14), LocalDate.of(2024, 1, 15))
        )
    }
}
