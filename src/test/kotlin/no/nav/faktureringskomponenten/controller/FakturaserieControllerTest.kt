package no.nav.faktureringskomponenten.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.faktureringskomponenten.controller.AuditorAwareFilter.Companion.NAV_USER_ID
import no.nav.faktureringskomponenten.controller.FakturaserieControllerIT.Companion.lagFakturaserieDto
import no.nav.faktureringskomponenten.controller.dto.FakturaseriePeriodeDto
import no.nav.faktureringskomponenten.controller.dto.NyFakturaserieResponseDto
import no.nav.faktureringskomponenten.featuretoggle.FeatureToggleConfig
import no.nav.faktureringskomponenten.service.FakturaserieService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.filter.CharacterEncodingFilter
import java.math.BigDecimal
import java.time.LocalDate

@ActiveProfiles("itest")
@Import(FeatureToggleConfig::class, FakturaserieControllerTest.TestConfig::class)
@WebMvcTest(FakturaserieController::class)
class FakturaserieControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val unleash: FakeUnleash,
) {

    @Autowired
    private lateinit var faktureringServiceMock: FakturaserieService


    @Test
    fun `Fakturaserie perioder kan ikke starte i tidligere år`() {
        unleash.enableAll()
        unleash.enable("melosys.faktureringskomponenten.ikke-tidligere-perioder")
        val lagFakturaserieDto = lagFakturaserieDto()


        mockMvc.perform(
            post("/fakturaserier")
                .contentType(MediaType.APPLICATION_JSON)
                .header(NAV_USER_ID, "testuser")
                .content(objectMapper.writeValueAsString(lagFakturaserieDto))
        )
            .andExpect(status().is4xxClientError())
            .andExpect { result ->
                val response = result.response.contentAsString
                val problemDetail = objectMapper.readValue(response, ProblemDetail::class.java)
                problemDetail.getViolationMessages() shouldHaveSize 1
                problemDetail.getViolationMessages() shouldContainOnly listOf("Startdato kan ikke være fra tidligere år")
            }

    }

    @Test
    fun `Fakturaserie perioder kan starte i nåværende år`() {
        unleash.enableAll()
        unleash.enable("melosys.faktureringskomponenten.ikke-tidligere-perioder")
        val lagFakturaserieDto = lagFakturaserieDto(
            fakturaseriePeriode = listOf(
                FakturaseriePeriodeDto(
                    BigDecimal.valueOf(123),
                    LocalDate.now().withDayOfYear(1),
                    LocalDate.now().withDayOfYear(100),
                    "Beskrivelse"
                )
            )
        )

        every { faktureringServiceMock.lagNyFakturaserie(any(), any()) } returns "123456"

        mockMvc.perform(
            post("/fakturaserier")
                .contentType(MediaType.APPLICATION_JSON)
                .header(NAV_USER_ID, "testuser")
                .content(objectMapper.writeValueAsString(lagFakturaserieDto))
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val response = result.response.contentAsString
                val nyFakturaserieResponseDto = objectMapper.readValue(response, NyFakturaserieResponseDto::class.java)
                nyFakturaserieResponseDto.fakturaserieReferanse shouldBe "123456"
            }
    }

    @Test
    fun `Fakturaserie perioder kan starte i tidligere år med toggle av`() {
        unleash.disableAll()
        unleash.disable("melosys.faktureringskomponenten.ikke-tidligere-perioder")
        val lagFakturaserieDto = lagFakturaserieDto()

        every { faktureringServiceMock.lagNyFakturaserie(any(), any()) } returns "123456"

        mockMvc.perform(
            post("/fakturaserier")
                .contentType(MediaType.APPLICATION_JSON)
                .header(NAV_USER_ID, "testuser")
                .content(objectMapper.writeValueAsString(lagFakturaserieDto))
        )
            .andExpect(status().isOk)
            .andExpect { result ->
                val response = result.response.contentAsString
                val nyFakturaserieResponseDto = objectMapper.readValue(response, NyFakturaserieResponseDto::class.java)
                nyFakturaserieResponseDto.fakturaserieReferanse shouldBe "123456"
            }
    }

    @TestConfiguration
    class TestConfig {

        @Bean
        @Primary
        fun characterEncodingFilter(): CharacterEncodingFilter {
            val filter = CharacterEncodingFilter()
            filter.encoding = "UTF-8"
            filter.setForceEncoding(true)
            return filter
        }

        @Bean
        @Primary
        fun fakturaserieService(): FakturaserieService {
            return mockk()
        }
    }

    fun ProblemDetail.getViolations(): List<Map<String, Any>> {
        return this.properties?.get("violations") as? List<Map<String, Any>> ?: emptyList()
    }

    fun ProblemDetail.getViolationMessages(): List<String> {
        return getViolations().mapNotNull { it["message"] as? String }
    }
}
