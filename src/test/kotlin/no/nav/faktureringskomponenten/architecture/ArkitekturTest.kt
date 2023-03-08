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
        .importPackages(appPath)

    val definedLayers = Architectures.layeredArchitecture().consideringAllDependencies()
        .layer("Controller").definedBy("$appPath.controller..")
        .layer("Service").definedBy("$appPath.service..")
        .layer("Domain").definedBy("$appPath.domain..")

    @Test
    fun `Ingen er avhengig av Controller laget`() {
        val layeringRules: ArchRule = definedLayers
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()

        layeringRules.check(importedClasses)
    }

    @Test
    fun `Service er bare brukt av Controller laget`() {
        val layeringRules: ArchRule = definedLayers
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")

        layeringRules.check(importedClasses)
    }

    @Test
    fun `Domain er bare brukt av Controller og Service lagene`() {
        val layeringRules: ArchRule = definedLayers
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Controller", "Service")

        layeringRules.check(importedClasses)
    }
}