package no.nav.faktureringskomponenten.domain.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import no.nav.faktureringskomponenten.domain.models.FakturaStatus

@Converter(autoApply = true)
class FakturaStatusConverter: AttributeConverter<FakturaStatus, String> {
    override fun convertToDatabaseColumn(fakturaStatus: FakturaStatus): String {
        return fakturaStatus.name
    }
    override fun convertToEntityAttribute(fakturaStatus: String): FakturaStatus {
        return FakturaStatus.values().first { r -> r == FakturaStatus.valueOf(fakturaStatus) }
    }
}