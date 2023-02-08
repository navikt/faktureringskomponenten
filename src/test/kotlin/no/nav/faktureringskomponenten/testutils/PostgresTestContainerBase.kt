package no.nav.faktureringskomponenten.testutils

import org.junit.jupiter.api.AfterEach
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@Import(value = [DBVerify::class])
open class PostgresTestContainerBase(
    private val dbVerify: DBVerify
) {
    companion object {
        var dbContainer = PostgreSQLContainer("postgres:12.11")

        @DynamicPropertySource
        @JvmStatic
        fun postgresProperties(registry: DynamicPropertyRegistry) {
            if (useTestContainer()) {
                registry.add("spring.datasource.url") { dbContainer.jdbcUrl }
                registry.add("spring.datasource.password") { dbContainer.password }
                registry.add("spring.datasource.username") { dbContainer.username }

                dbContainer.start()
            }
        }

        private fun useTestContainer(): Boolean =
            System.getenv("USE-LOCAL-DB")?.lowercase() != "true"
    }

    private fun checkThatDatabaseIsEmpty() {
        if (useTestContainer()) {
            dbVerify.databaseShouldBeClean()
        }
    }

    @AfterEach
    fun postgresTestContainerBaseAfterEach() {
        checkThatDatabaseIsEmpty()
    }
}
