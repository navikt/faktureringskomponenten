package no.nav.faktureringskomponenten.service

import no.nav.faktureringskomponenten.domain.models.Faktura
import no.nav.faktureringskomponenten.domain.models.FakturaseriePeriode
import org.springframework.stereotype.Component

@Component
class AvregningBehandler {
    fun lagAvregningsfaktura(bestilteFakturaer: List<Faktura>, fakturaseriePerioder: List<FakturaseriePeriode>): Faktura? {
        val overlappendeFakturaer = finnOverlappendeFakturaer(bestilteFakturaer, fakturaseriePerioder)
        return if (overlappendeFakturaer.isNotEmpty()) AvregningsfakturaGenerator().lagFaktura(fakturaseriePerioder, overlappendeFakturaer) else null
    }

    private fun finnOverlappendeFakturaer(bestilteFakturaer: List<Faktura>, fakturaseriePerioder: List<FakturaseriePeriode>): List<Faktura> {
        return bestilteFakturaer
    }
}
