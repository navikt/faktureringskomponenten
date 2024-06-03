package no.nav.faktureringskomponenten.controller

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.faktureringskomponenten.controller.dto.BeregnTotalBeløpDto
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import no.nav.faktureringskomponenten.service.beregning.BeløpBeregner

import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(SpringExtension::class, MockKExtension::class)
@WebMvcTest(TotalBeløpController::class)
class TotalBeløpControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `test hentTotalBeløp beregner totalt beløp`() {
        val fakturaseriePerioder = listOf(
            FakturaseriePeriode(
                enhetsprisPerManed = BigDecimal(1000.00),
                startDato = LocalDate.parse("2022-01-01"),
                sluttDato = LocalDate.parse("2022-03-31"),
                beskrivelse = "Sample description"
            ),
            FakturaseriePeriode(
                enhetsprisPerManed = BigDecimal(1000.00),
                startDato = LocalDate.parse("2022-04-01"),
                sluttDato = LocalDate.parse("2022-06-30"),
                beskrivelse = "Sample description"
            )
        )
        val dto = BeregnTotalBeløpDto(fakturaseriePerioder)

        mockMvc.perform(post("/totalbeloep/beregn")
                .header("Nav-User-Id", "test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isOk)
            .andExpect(content().string("6000.00"))
    }
}
