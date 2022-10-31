package no.nav.melosysfakturering.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity


@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Autowired
    @Throws(Exception::class)
    protected fun configure(http: HttpSecurity) {
        http
            .authorizeRequests()
            .anyRequest()
            .permitAll()
    }
}