package no.nav.faktureringskomponenten.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.library.Architectures
import org.junit.jupiter.api.Test
class ArkitekturTest {

    val appPath = "no.nav.faktureringskomponenten"

    val importedClasses: JavaClasses = ClassFileImporter()
        .withImportOption(DoNotIncludeTests())
        .importClasspath()

    val definedLayers = Architectures.layeredArchitecture().consideringAllDependencies()
        .layer("Controller").definedBy("$appPath.controller..")
        .layer("Service").definedBy("$appPath.service..")
        .layer("Domain").definedBy("$appPath.domain..")
        .layer("Metrics").definedBy("$appPath.metrics..")

    @Test
    fun `Controller er ikke brukt av noen andre lag`() {
        val layeringRules: ArchRule = definedLayers
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()

        layeringRules.check(importedClasses)
    }

    @Test
    fun `Service er bare brukt av Controller laget`() {
        val layeringRules: ArchRule = definedLayers
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Metrics")

        layeringRules.check(importedClasses)
    }

    @Test
    fun `Domain er bare brukt av Controller eller Service lagene`() {
        val layeringRules: ArchRule = definedLayers
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Controller", "Service")

        layeringRules.check(importedClasses)
    }
}