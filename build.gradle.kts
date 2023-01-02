import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    kotlin("plugin.jpa") version "1.6.21"
}

group = "no.nav"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

object dependencyVersions {
    const val zalandoProblemVersion = "0.27.0"
    const val testContainerVersion = "1.17.6"
    const val kotestVersion = "5.5.4"
    const val shedlockVersion = "4.4.0"
    const val shedlockProvicerJdbcVersion = "4.43.0"
    const val mockkVersion = "1.13.3"
    const val openapiVersion = "1.6.0"
    const val tokenSupportVersion = "2.0.20"
    const val micrometerJvmExtrasVersion = "0.2.2"
    const val micrometerVersion = "1.7.10"
}

object TestContainersDependencies {
    const val postgresTestContainers = "org.testcontainers:postgresql:${dependencyVersions.testContainerVersion}"
    const val junitJupiterTestContainers = "org.testcontainers:junit-jupiter:${dependencyVersions.testContainerVersion}"
}

dependencies {
    implementation("org.springdoc:springdoc-openapi-data-rest:${dependencyVersions.openapiVersion}")
    implementation("org.springdoc:springdoc-openapi-ui:${dependencyVersions.openapiVersion}")
    implementation("org.springdoc:springdoc-openapi-kotlin:${dependencyVersions.openapiVersion}")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("no.nav.security:token-validation-spring:${dependencyVersions.tokenSupportVersion}")
    implementation("org.zalando:problem-spring-web-starter:${dependencyVersions.zalandoProblemVersion}")
    implementation("org.flywaydb:flyway-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("net.javacrumbs.shedlock:shedlock-spring:${dependencyVersions.shedlockVersion}")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:${dependencyVersions.shedlockProvicerJdbcVersion}")
    implementation("io.micrometer:micrometer-registry-prometheus:${dependencyVersions.micrometerVersion}")
    implementation("io.github.mweirauch:micrometer-jvm-extras:${dependencyVersions.micrometerJvmExtrasVersion}")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("no.nav.security:token-validation-spring-test:${dependencyVersions.tokenSupportVersion}")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-runner-junit5:${dependencyVersions.kotestVersion}")
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

