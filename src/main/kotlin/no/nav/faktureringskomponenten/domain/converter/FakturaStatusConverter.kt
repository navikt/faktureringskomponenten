package no.nav.faktureringskomponenten.domain.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import no.nav.faktureringskomponenten.domain.models.FakturaStatus

@Converter(autoApply = true)
class FakturaStatusConverter : AttributeConverter<FakturaStatus?, String?> {
    override fun convertToDatabaseColumn(fakturaStatus: FakturaStatus?): String? {
        return fakturaStatus?.toString()
    }

    override fun convertToEntityAttribute(code: String?): FakturaStatus? {
        return if (code != null) FakturaStatus.valueOf(code) else null
    }
}