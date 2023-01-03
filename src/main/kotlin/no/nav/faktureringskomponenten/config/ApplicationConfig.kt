package no.nav.faktureringskomponenten.config

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Configuration

@Configuration
@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox.documentation"])
class ApplicationConfig {
}