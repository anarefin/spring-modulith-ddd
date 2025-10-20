package com.demo.modular.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * ArchUnit tests to enforce architectural boundaries and best practices.
 */
class ModuleArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.demo.modular");
    }

    @Test
    void controllersShouldOnlyDependOnServices() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..api..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..service..", "..api..", "..domain..", "java..", "org.springframework..", "lombok..", "jakarta..", "org.slf4j..");

        rule.check(importedClasses);
    }

    @Test
    void controllersShouldNotAccessRepositories() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..api..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..repository..");

        rule.check(importedClasses);
    }

    @Test
    void servicesShouldNotDependOnControllers() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..service..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..api..")
                .andShould().haveSimpleNameEndingWith("Controller");

        rule.check(importedClasses);
    }

    @Test
    void repositoriesShouldOnlyBeAccessedByServices() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..repository..")
                .and().haveSimpleNameEndingWith("Repository")
                .should().onlyBeAccessed().byAnyPackage("..service..", "..repository..");

        rule.check(importedClasses);
    }

    @Test
    void domainModelsShouldNotDependOnOtherLayers() {
        // Note: Domain entities can depend on Status enums in api.dto since they're part of the public contract
        ArchRule rule = classes()
                .that().resideInAnyPackage("..domain..", "..internal.domain..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..domain..", "..internal.domain..", "..api.dto..", "java..", "jakarta..", "lombok..", "org.springframework.data..");

        rule.check(importedClasses);
    }

    @Test
    void serviceImplementationsShouldNotBePublic() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..service..")
                .and().haveSimpleNameEndingWith("ServiceImpl")
                .should().notBePublic();

        rule.check(importedClasses);
    }

    @Test
    void serviceInterfacesShouldBePublic() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..service..")
                .and().areInterfaces()
                .and().haveSimpleNameEndingWith("Service")
                .should().bePublic();

        rule.check(importedClasses);
    }

    @Test
    void noClassesShouldDependOnImplementationDetails() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .haveSimpleNameEndingWith("Impl")
                .orShould().dependOnClassesThat()
                .haveSimpleNameEndingWith("Implementation");

        rule.check(importedClasses);
    }

    @Test
    void layeredArchitectureShouldBeRespected() {
        ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controllers").definedBy("..api..")
                .layer("Services").definedBy("..service..", "..internal.service..")
                .layer("Repositories").definedBy("..repository..", "..internal.repository..")
                .layer("Domain").definedBy("..domain..", "..internal.domain..")
                
                .whereLayer("Controllers").mayNotBeAccessedByAnyLayer()
                .whereLayer("Services").mayOnlyBeAccessedByLayers("Controllers", "Services")
                .whereLayer("Repositories").mayOnlyBeAccessedByLayers("Services")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Services", "Repositories", "Controllers");

        rule.check(importedClasses);
    }

    @Test
    void moduleShouldBeFreeOfCycles() {
        ArchRule rule = slices()
                .matching("com.demo.modular.(*)..")
                .should().beFreeOfCycles();

        rule.check(importedClasses);
    }

    @Test
    void controllersShouldBeAnnotatedWithRestController() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..api..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class);

        rule.check(importedClasses);
    }

    @Test
    void servicesShouldBeAnnotatedWithService() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..service..")
                .and().haveSimpleNameEndingWith("ServiceImpl")
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class);

        rule.check(importedClasses);
    }

    @Test
    void repositoriesShouldBeInterfaces() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..repository..")
                .and().haveSimpleNameEndingWith("Repository")
                .should().beInterfaces();

        rule.check(importedClasses);
    }

    @Test
    void exceptionsShouldResideInApiPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Exception")
                .and().resideInAnyPackage("com.demo.modular..")
                .should().resideInAnyPackage("..api.exception..");

        rule.check(importedClasses);
    }

    @Test
    void dtosShouldResideInApiPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("DTO")
                .and().resideInAnyPackage("com.demo.modular..")
                .should().resideInAnyPackage("..api.dto..");

        rule.check(importedClasses);
    }

    @Test
    void mappersShouldBePackagePrivate() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .and().resideInAnyPackage("..service..", "..internal.service..")
                .should().notBePublic();

        rule.check(importedClasses);
    }

    // ========== Internal Package Rules ==========

    @Test
    void internalPackagesShouldNotBeAccessedFromOtherModules() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("com.demo.modular.product..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.demo.modular.order.internal..", "com.demo.modular.payment.internal..");

        rule.check(importedClasses);

        // Order module should not access internal packages of other modules
        rule = noClasses()
                .that().resideInAnyPackage("com.demo.modular.order..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.demo.modular.product.internal..", "com.demo.modular.payment.internal..");

        rule.check(importedClasses);

        // Payment module should not access internal packages of other modules
        rule = noClasses()
                .that().resideInAnyPackage("com.demo.modular.payment..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.demo.modular.product.internal..", "com.demo.modular.order.internal..");

        rule.check(importedClasses);
    }

    @Test
    void internalClassesShouldNotBePublic() {
        // Note: JPA entities must be public for Hibernate/Spring Data, so we exempt them
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..internal..")
                .and().areNotInterfaces()
                .and().areNotEnums()
                .and().areNotAnnotatedWith(jakarta.persistence.Entity.class)  // Exempt JPA entities
                .should().bePublic()
                .because("Internal implementation classes should have package-private visibility (except JPA entities)");

        rule.check(importedClasses);
    }

    @Test
    void domainEntitiesShouldBeInInternalPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().resideInAnyPackage("..internal.domain..")
                .because("JPA entities are internal implementation details");

        rule.check(importedClasses);
    }

    @Test
    void repositoriesShouldBeInInternalPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .and().areNotInterfaces().or().areAnnotatedWith(org.springframework.stereotype.Repository.class)
                .should().resideInAnyPackage("..internal.repository..")
                .because("Repositories are internal implementation details");

        rule.check(importedClasses);
    }

    @Test
    void serviceImplementationsShouldBeInInternalPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("ServiceImpl")
                .should().resideInAnyPackage("..internal.service..")
                .because("Service implementations are internal details");

        rule.check(importedClasses);
    }

    @Test
    void mappersShouldBeInInternalPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Mapper")
                .should().resideInAnyPackage("..internal.service..")
                .because("Mappers are internal utilities");

        rule.check(importedClasses);
    }

    @Test
    void internalPackagesShouldOnlyContainImplementationDetails() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..internal..")
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .because("Internal packages should not contain REST controllers");

        rule.check(importedClasses);

        // No DTOs in internal
        rule = noClasses()
                .that().resideInAnyPackage("..internal..")
                .should().haveSimpleNameEndingWith("DTO")
                .because("DTOs belong in public API package, not internal");

        rule.check(importedClasses);

        // No public exceptions in internal
        rule = noClasses()
                .that().resideInAnyPackage("..internal..")
                .should().haveSimpleNameEndingWith("Exception")
                .because("Exceptions should be in public API package, not internal");

        rule.check(importedClasses);
    }

    @Test
    void publicServiceInterfacesShouldNotBeInInternalPackage() {
        // Check that there are no public service interfaces in internal package
        // Service interfaces should be in the service package, not internal
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..internal..")
                .and().areInterfaces()
                .and().haveSimpleNameEndingWith("Service")
                .should().bePublic()
                .allowEmptyShould(true)  // Allow passing when no classes match (which is the desired state)
                .because("Public service interfaces belong in the public service package, not internal");

        rule.check(importedClasses);
    }

    @Test
    void onlyApiAndServicePackagesShouldBeAccessibleFromOtherModules() {
        // Product module's internal should not be accessed from outside
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("com.demo.modular.product..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.demo.modular.product.internal..")
                .because("Internal packages should not be accessible from other modules");

        rule.check(importedClasses);

        // Order module's internal should not be accessed from outside
        rule = noClasses()
                .that().resideOutsideOfPackage("com.demo.modular.order..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.demo.modular.order.internal..")
                .because("Internal packages should not be accessible from other modules");

        rule.check(importedClasses);

        // Payment module's internal should not be accessed from outside
        rule = noClasses()
                .that().resideOutsideOfPackage("com.demo.modular.payment..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("com.demo.modular.payment.internal..")
                .because("Internal packages should not be accessible from other modules");

        rule.check(importedClasses);
    }
}

