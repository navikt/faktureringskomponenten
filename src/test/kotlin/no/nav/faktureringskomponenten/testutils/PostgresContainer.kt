package no.nav.faktureringskomponenten.testutils

import org.testcontainers.containers.PostgreSQLContainer

class PostgresContainer : PostgreSQLContainer<PostgresContainer?>("postgres:12.3") {

    companion object {
        fun setupAndStart(): PostgresContainer {
            return PostgresContainer().apply {
                start()
                System.setProperty("spring.datasource.url", jdbcUrl)
                System.setProperty("spring.datasource.username", username)
                System.setProperty("spring.datasource.password", password)
            }
        }
    }
}
