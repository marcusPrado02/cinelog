package com.cine.cinelog.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Testes de arquitetura usando ArchUnit para garantir enforcement de Clean
 * Architecture.
 *
 * <p>
 * Valida que:
 * </p>
 * <ul>
 * <li>Domain não depende de camadas externas (web, persistence,
 * integration)</li>
 * <li>Application não depende de adapters concretos</li>
 * <li>Controllers não importam domain models diretamente</li>
 * <li>Respeita o fluxo de dependências: web → application → domain</li>
 * </ul>
 *
 * @since 1.1.0
 */
@DisplayName("Clean Architecture - Enforcement com ArchUnit")
class LayeredArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.cine.cinelog");
    }

    @Test
    @DisplayName("Domain não deve depender de nenhuma camada externa")
    void domainShouldNotDependOnExternalLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..core.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..features..web..",
                        "..features..persistence..",
                        "..infrastructure..",
                        "..shared.web..",
                        "..shared.persistence..")
                .because("Domain deve ser independente de frameworks e tecnologias externas");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Domain não deve usar anotações de framework")
    void domainShouldNotUseFrameworkAnnotations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..core.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.validation..")
                .because("Domain deve ser livre de dependências de framework");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Application não deve depender de adapters concretos")
    void applicationShouldNotDependOnAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..core.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..features..persistence..",
                        "..features..web..",
                        "..infrastructure.persistence..",
                        "..infrastructure.web..",
                        "..infrastructure.messaging..")
                .because("Application deve depender apenas de ports (interfaces), não de adapters concretos");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Controllers não devem importar domain models diretamente")
    void controllersShouldNotImportDomainModels() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..features..web.controller..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..core.domain.model..")
                .because("Controllers devem usar DTOs e chamar UseCases/Ports, não acessar domain models diretamente");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Validar arquitetura em camadas com fluxo correto de dependências")
    void layeredArchitectureShouldBeRespected() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()

                .layer("Domain").definedBy("..core.domain..")
                .layer("Application").definedBy("..core.application..")
                .layer("Adapters").definedBy("..features..", "..infrastructure..")
                .layer("Web").definedBy("..web..")
                .layer("Persistence").definedBy("..persistence..")

                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Web")
                .whereLayer("Persistence").mayNotBeAccessedByAnyLayer()
                .whereLayer("Web").mayNotBeAccessedByAnyLayer()

                .check(importedClasses);
    }

    @Test
    @DisplayName("UseCases devem terminar com UseCase")
    void useCasesShouldFollowNamingConvention() {
        ArchRule rule = classes()
                .that().resideInAPackage("..core.application.usecases..")
                .should().haveSimpleNameEndingWith("UseCase")
                .orShould().haveSimpleNameEndingWith("Service")
                .because("UseCases devem seguir convenção de nomenclatura");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Ports devem ser interfaces")
    void portsShouldBeInterfaces() {
        ArchRule rule = classes()
                .that().resideInAPackage("..core.application.ports..")
                .should().beInterfaces()
                .because("Ports devem ser interfaces para permitir inversão de dependência");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Controllers devem usar anotação @RestController")
    void controllersShouldBeAnnotatedWithRestController() {
        ArchRule rule = classes()
                .that().resideInAPackage("..features..web.controller..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .because("Controllers REST devem usar @RestController");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Repositories devem terminar com Repository")
    void repositoriesShouldFollowNamingConvention() {
        ArchRule rule = classes()
                .that().resideInAPackage("..persistence..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("Repository")
                .because("Repositories devem seguir convenção de nomenclatura");

        rule.check(importedClasses);
    }
}
