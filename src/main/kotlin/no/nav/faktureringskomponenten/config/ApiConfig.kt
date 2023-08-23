package no.nav.faktureringskomponenten.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class ApiConfig : WebMvcConfigurer{

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(CorrelationIdInterceptor())
    }
}