package no.nav.faktureringskomponenten.config.metrics

import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheus.PrometheusRenameFilter
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig {

    @Bean
    fun metricsCommonTags(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry: MeterRegistry ->
            registry.config().meterFilter(PrometheusRenameFilter())
                .commonTags("app", "faktureringskomponenten", "team", "teammelosys")
        }
    }

    @Bean
    fun processMemoryMetrics(): MeterBinder? {
        return ProcessMemoryMetrics()
    }

    @Bean
    fun processThreadMetrics(): MeterBinder? {
        return ProcessThreadMetrics()
    }
}