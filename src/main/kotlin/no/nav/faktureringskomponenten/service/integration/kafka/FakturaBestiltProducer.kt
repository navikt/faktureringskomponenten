package no.nav.faktureringskomponenten.service.integration.kafka

import no.nav.faktureringskomponenten.service.integration.kafka.dto.FakturaBestiltDto

fun interface FakturaBestiltProducer {
    fun produserBestillingsmelding(fakturaBestiltDto: FakturaBestiltDto)
}