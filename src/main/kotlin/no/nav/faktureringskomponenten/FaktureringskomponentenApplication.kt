package no.nav.faktureringskomponenten

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableJwtTokenValidation
class FaktureringskomponentenApplication

fun main(args: Array<String>) {
	runApplication<FaktureringskomponentenApplication>(*args)
}
