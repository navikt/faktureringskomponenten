package no.nav.faktureringskomponenten.featuretoggle


import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.getunleash.strategy.GradualRolloutRandomStrategy
import io.getunleash.strategy.GradualRolloutSessionIdStrategy
import io.getunleash.strategy.GradualRolloutUserIdStrategy
import io.getunleash.strategy.UserWithIdStrategy
import io.getunleash.util.UnleashConfig
import no.nav.melosys.featuretoggle.LocalUnleash
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import java.util.*


@Configuration
class FeatureToggleConfig {
    private val UNLEASH_URL = "https://melosys-unleash-api.nav.cloud.nais.io/api"
    private val APP_NAME = "faktureringskomponenten"
    @Bean
    fun unleash(environment: Environment, @Value("\${unleash.token}") token: String): Unleash {
        return if (!Collections.disjoint(
                listOf(*environment.activeProfiles),
                listOf("local")
            )
        ) {
            val localUnleash = LocalUnleash()
            localUnleash.enableAll()
            localUnleash
        } else if (listOf(*environment.activeProfiles).contains("itest")) {
            val fakeUnleash = FakeUnleash()
            fakeUnleash.disableAll()
            fakeUnleash
        } else {
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
            )
        }
    }
}
