package no.nav.faktureringskomponenten.service.integration.kafka.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.faktureringskomponenten.domain.models.FakturaStatus
import no.nav.faktureringskomponenten.service.integration.kafka.dto.EksternFakturaStatusDto
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class DeserializerJsonAwareTest {

    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
    private val deserializer = DeserializerJsonAware()
    private val topic = "test-topic"

    @Test
    fun `deserializer parser gyldig JSON til EksternFakturaStatusDto`() {
        val dto = EksternFakturaStatusDto(
            fakturaReferanseNr = "ref-123",
            fakturaNummer = "82",
            dato = LocalDate.of(2024, 1, 15),
            status = FakturaStatus.INNE_I_OEBS,
            fakturaBelop = BigDecimal("4000.00"),
            ubetaltBelop = BigDecimal("2000.00"),
            feilmelding = null
        )
        val json = objectMapper.writeValueAsString(dto)

        val result = deserializer.deserialize(topic, json.toByteArray())

        result?.fakturaReferanseNr shouldBe dto.fakturaReferanseNr
        result?.fakturaNummer shouldBe dto.fakturaNummer
        result?.dato shouldBe dto.dato
        result?.status shouldBe dto.status
        result?.fakturaBelop shouldBe dto.fakturaBelop
        result?.ubetaltBelop shouldBe dto.ubetaltBelop
        result?.feilmelding shouldBe null
    }

    @Test
    fun `deserializer lagrer rå JSON-streng`() {
        val dto = EksternFakturaStatusDto(
            fakturaReferanseNr = "ref-456",
            fakturaNummer = null,
            dato = LocalDate.of(2024, 6, 1),
            status = FakturaStatus.FEIL,
            fakturaBelop = null,
            ubetaltBelop = null,
            feilmelding = "Feil fra OEBS"
        )
        val json = objectMapper.writeValueAsString(dto)

        deserializer.deserialize(topic, json.toByteArray())

        deserializer.json shouldBe json
    }

    @Test
    fun `deserializer returnerer null og nullstiller json ved null input`() {
        val result = deserializer.deserialize(topic, null)

        result.shouldBeNull()
        deserializer.json.shouldBeNull()
    }
}
