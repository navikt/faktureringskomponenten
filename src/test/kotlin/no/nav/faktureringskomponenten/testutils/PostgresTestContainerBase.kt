package no.nav.faktureringskomponenten.testutils

import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@Import(value = [DBVerify::class])
open class PostgresTestContainerBase {

    @Suppress("SpringJavaAutowiredMembersInspection")
    @Autowired
    private lateinit var dbVerify: DBVerify

    private var dbCleanUpActions = mutableListOf<() -> Unit>()

    companion object {
        var dbContainer = PostgreSQLContainer("postgres:12.11")
        private const val useContainer = true // easy way to switch to run against local docker

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
            System.getenv("USE-LOCAL-DB")?.lowercase() != "true" && useContainer
    }

    protected fun addCleanUpAction(deleteAction: () -> Unit) {
        dbCleanUpActions.add(deleteAction)
    }

    @AfterEach
    fun postgresTestContainerBaseAfterEach() {
        dbCleanUpActions.forEach { it() }
    }
}
