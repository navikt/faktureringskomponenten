import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "2.3.10"

    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("plugin.allopen") version kotlinVersion
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17
extra["junit-jupiter.version"] = "5.11.3"
extra["testcontainers.version"] = "2.0.3"

repositories {
    mavenCentral()
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

tasks.test {
    jvmArgs(
        "--add-opens", "java.base/java.time=ALL-UNNAMED",
    )
}

object dependencyVersions {
    const val testContainerVersion = "2.0.3"
    const val kotestVersion = "5.5.4"
    const val shedlockVersion = "4.4.0"
    const val shedlockProvicerJdbcVersion = "4.43.0"
    const val mockkVersion = "1.13.3"
    const val springdocOpenapiStarter = "2.8.14"
    const val logstashLogbackEncoder = "7.2"
    const val tokenSupportVersion = "3.2.0"
    const val awaitabilityVersion = "4.2.0"
    const val kotlinLogging = "3.0.5"
    const val archUnitVersion = "1.0.1"
    const val micrometerJvmExtrasVersion = "0.2.2"
    const val threeTenExtraVersion = "1.7.2"
    const val unleashVersion = "8.3.0"
    const val ULIDVersion = "1.3.0"
}

dependencies {
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${dependencyVersions.springdocOpenapiStarter}")
    implementation("org.springdoc:springdoc-openapi-starter-common:${dependencyVersions.springdocOpenapiStarter}")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv")
    implementation("io.github.microutils:kotlin-logging-jvm:${dependencyVersions.kotlinLogging}")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.github.mweirauch:micrometer-jvm-extras:${dependencyVersions.micrometerJvmExtrasVersion}")
    implementation("io.getunleash:unleash-client-java:${dependencyVersions.unleashVersion}")
    implementation("net.javacrumbs.shedlock:shedlock-spring:${dependencyVersions.shedlockVersion}")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:${dependencyVersions.shedlockProvicerJdbcVersion}")
    implementation("net.logstash.logback:logstash-logback-encoder:${dependencyVersions.logstashLogbackEncoder}")
    implementation("no.nav.security:token-validation-spring:${dependencyVersions.tokenSupportVersion}")
    implementation("org.flywaydb:flyway-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.threeten:threeten-extra:${dependencyVersions.threeTenExtraVersion}")
    implementation("com.aallam.ulid:ulid-kotlin:${dependencyVersions.ULIDVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito", module = "mockito-core")
    }
    testImplementation("no.nav.security:token-validation-spring-test:${dependencyVersions.tokenSupportVersion}")
    testImplementation("io.kotest:kotest-assertions-core-jvm:${dependencyVersions.kotestVersion}")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("io.mockk:mockk:${dependencyVersions.mockkVersion}")
    testImplementation("org.testcontainers:testcontainers-postgresql:${dependencyVersions.testContainerVersion}")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:${dependencyVersions.testContainerVersion}")
    testImplementation("org.awaitility:awaitility:${dependencyVersions.awaitabilityVersion}")
    testImplementation("org.awaitility:awaitility-kotlin:${dependencyVersions.awaitabilityVersion}")
    testImplementation("com.tngtech.archunit:archunit:${dependencyVersions.archUnitVersion}")
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict")
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    }

    getByName<Jar>("jar") {
        enabled = false
    }
}

