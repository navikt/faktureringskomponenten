package no.nav.faktureringskomponenten.service.mappers

import no.nav.faktureringskomponenten.domain.models.FakturaserieTema

class FakturaserieTemaTilArtikkelMapper {
    val AVGIFT_TIL_FOLKETRYGDEN: String = "F00008"

    fun tilArtikkel(tema: FakturaserieTema): String {
        return when (tema) {
            FakturaserieTema.TRY -> AVGIFT_TIL_FOLKETRYGDEN
        }
    }
}