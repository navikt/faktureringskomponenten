package no.nav.faktureringskomponenten.featuretoggle


import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.getunleash.strategy.GradualRolloutRandomStrategy
import io.getunleash.strategy.GradualRolloutSessionIdStrategy
import io.getunleash.strategy.GradualRolloutUserIdStrategy
import io.getunleash.strategy.UserWithIdStrategy
import io.getunleash.util.UnleashConfig
import mu.KotlinLogging
import no.nav.melosys.featuretoggle.LocalUnleash
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.util.*

private val log = KotlinLogging.logger {}

@Configuration
class FeatureToggleConfig {
    private val UNLEASH_URL = "https://melosys-unleash-api.nav.cloud.nais.io/api"
    private val APP_NAME = "faktureringskomponenten"

    @Bean
    fun unleash(
        environment: Environment,
        @Value("\${unleash.token}") token: String,
        @Value("\${unleash.url:}") unleashUrl: String
    ): Unleash {
        return if (!Collections.disjoint(
                listOf(*environment.activeProfiles),
                listOf("local")
            )
        ) {
            // Local profile
            if (unleashUrl.isNotBlank()) {
                // Connect to Unleash server (e.g., Docker Compose) with default-enabled wrapper
                val config = UnleashConfig.builder()
                    .apiKey(token)
                    .appName("$APP_NAME-local")
                    .unleashAPI(unleashUrl)
                    .build()

                val defaultUnleash = DefaultUnleash(config,
                    GradualRolloutSessionIdStrategy(),
                    GradualRolloutUserIdStrategy(),
                    GradualRolloutRandomStrategy(),
                    UserWithIdStrategy(),
                    ByUserIdStrategy()
                )
                DefaultEnabledUnleash(defaultUnleash).also {
                    log.info { "FeatureToggleConfig: Using DefaultEnabledUnleash wrapping Unleash server at $unleashUrl" }
                }
            } else {
                // Fallback to LocalUnleash if no server configured
                val localUnleash = LocalUnleash()
                localUnleash.enableAll()
                localUnleash.also {
                    log.info { "FeatureToggleConfig: Using LocalUnleash (no Unleash server configured)" }
                }
            }
        } else if (listOf(*environment.activeProfiles).contains("itest")) {
            // Integration test profile
            val fakeUnleash = FakeUnleash()
            fakeUnleash.disableAll()
            fakeUnleash
        } else {
            // NAIS profile (production/dev)
            val unleashConfig: UnleashConfig = UnleashConfig.builder()
                .apiKey(token)
                .appName(APP_NAME)
                .unleashAPI(UNLEASH_URL)
                .build()
            DefaultUnleash(
                unleashConfig,
                GradualRolloutSessionIdStrategy(),
                GradualRolloutUserIdStrategy(),
                GradualRolloutRandomStrategy(),
                UserWithIdStrategy(),
                ByUserIdStrategy()
            )
        }
    }
}
