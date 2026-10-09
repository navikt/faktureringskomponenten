package no.nav.faktureringskomponenten.config

import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class ApiConfig(
    // Tom verdi gir 403 på /admin/**, se application.yml
    @param:Value("\${admin.driftsgruppe}") private val driftsgruppeId: String,
    @param:Value("\${admin.console-klient-id}") private val consoleKlientId: String,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(CorrelationIdInterceptor())
        // Etter CorrelationIdInterceptor, så avvisninger logges med korrelasjons-ID.
        // Lages her og ikke som @Component, så @WebMvcTest uten token-support fortsatt starter.
        registry.addInterceptor(
            AdminTilgangInterceptor(SpringTokenValidationContextHolder(), driftsgruppeId, consoleKlientId)
        ).addPathPatterns("/admin/**")
    }
}
