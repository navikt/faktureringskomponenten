package no.nav.faktureringskomponenten.testutils

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

open class PostgresTestContainerBase {
    companion object {
        var dbContainer = PostgreSQLContainer("postgres:12.11")

        @DynamicPropertySource
        @JvmStatic
        fun postgresProperties(registry: DynamicPropertyRegistry) {
            val env: String? = System.getenv("USE-LOCAL-DB")
            if (env?.lowercase() == "true") return

            registry.add("spring.datasource.url") { dbContainer.jdbcUrl }
            registry.add("spring.datasource.password") { dbContainer.password }
            registry.add("spring.datasource.username") { dbContainer.username }

            dbContainer.start()
        }
    }
}
