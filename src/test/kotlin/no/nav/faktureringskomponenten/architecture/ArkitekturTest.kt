package no.nav.faktureringskomponenten.architecture

import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures
import org.junit.jupiter.api.Test
class ArkitekturTest {

    val appPath = "no.nav.faktureringskomponenten"

    val importedClasses: JavaClasses = ClassFileImporter()
        .withImportOption(DoNotIncludeTests())
        .importPackages(appPath)

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

    @Test
    fun `Statistikk er isolert fra resten av appen i begge retninger`() {
        val statistikk = "$appPath.statistikk.."

        noClasses().that().resideInAPackage(statistikk)
            .should().dependOnClassesThat(resideInAPackage("$appPath..").and(resideOutsideOfPackage(statistikk)))
            .check(importedClasses)

        noClasses().that().resideOutsideOfPackage(statistikk)
            .should().dependOnClassesThat().resideInAPackage(statistikk)
            .check(importedClasses)
    }
}