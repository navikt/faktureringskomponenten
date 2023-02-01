import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "1.6.21"

    id("org.springframework.boot") version "3.0.1"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

object dependencyVersions {
    const val testContainerVersion = "1.17.6"
    const val kotestVersion = "5.5.4"
    const val shedlockVersion = "4.4.0"
    const val shedlockProvicerJdbcVersion = "4.43.0"
    const val mockkVersion = "1.13.3"
    const val openapiVersion = "1.6.14"
    const val springdocOpenapiStarter = "2.0.2"
    const val logstashLogbackEncoder = "7.2"
    const val tokenSupportVersion = "3.0.2"
}

object TestContainersDependencies {
    const val postgresTestContainers = "org.testcontainers:postgresql:${dependencyVersions.testContainerVersion}"
    const val junitJupiterTestContainers = "org.testcontainers:junit-jupiter:${dependencyVersions.testContainerVersion}"
}

dependencies {
    implementation("org.springdoc:springdoc-openapi-kotlin:${dependencyVersions.openapiVersion}")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${dependencyVersions.springdocOpenapiStarter}")
    implementation("org.springdoc:springdoc-openapi-starter-common:${dependencyVersions.springdocOpenapiStarter}")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("no.nav.security:token-validation-spring:${dependencyVersions.tokenSupportVersion}")
    implementation("org.flywaydb:flyway-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("net.javacrumbs.shedlock:shedlock-spring:${dependencyVersions.shedlockVersion}")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:${dependencyVersions.shedlockProvicerJdbcVersion}")
    implementation("net.logstash.logback:logstash-logback-encoder:${dependencyVersions.logstashLogbackEncoder}")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("no.nav.security:token-validation-spring-test:${dependencyVersions.tokenSupportVersion}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-assertions-core-jvm:${dependencyVersions.kotestVersion}")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("io.mockk:mockk:${dependencyVersions.mockkVersion}")
    testImplementation(TestContainersDependencies.postgresTestContainers)
    testImplementation(TestContainersDependencies.junitJupiterTestContainers)
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    getByName<Jar>("jar") {
        enabled = false
    }
}

