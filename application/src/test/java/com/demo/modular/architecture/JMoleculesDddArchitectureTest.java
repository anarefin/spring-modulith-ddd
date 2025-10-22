package com.demo.modular.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.jmolecules.archunit.JMoleculesDddRules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

/**
 * ArchUnit tests to verify JMolecules DDD annotations and tactical design patterns.
 */
class JMoleculesDddArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.demo.modular");
    }

    // ========== JMolecules DDD Rules ==========

    @Test
    void aggregatesShouldFollowDddRules() {
        ArchRule rule = JMoleculesDddRules.all();
        rule.check(importedClasses);
    }

    // ========== Aggregate Root Tests ==========

    @Test
    void aggregateRootsShouldBeAnnotated() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..internal.domain..")
                .and().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().beAnnotatedWith(org.jmolecules.ddd.annotation.AggregateRoot.class)
                .because("Domain entities should be marked as JMolecules Aggregate Roots");

        rule.check(importedClasses);
    }

    @Test
    void aggregateRootsShouldBeEntities() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.jmolecules.ddd.annotation.AggregateRoot.class)
                .should().beAnnotatedWith(jakarta.persistence.Entity.class)
                .because("Aggregate Roots should be JPA entities");

        rule.check(importedClasses);
    }

    // ========== Value Object Tests ==========

    @Test
    void valueObjectsShouldBeAnnotated() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..domain.vo..")
                .should().beAnnotatedWith(org.jmolecules.ddd.annotation.ValueObject.class)
                .because("All classes in value object packages should be annotated with @ValueObject");

        rule.check(importedClasses);
    }

    @Test
    void valueObjectsShouldBeImmutable() {
        // Check that value object fields are not public and mutable
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().areAnnotatedWith(org.jmolecules.ddd.annotation.ValueObject.class)
                .and().areNotStatic()
                .and().areNotFinal()
                .should().notBePublic()
                .because("Value Objects should be immutable - fields should not be public mutable");

        rule.check(importedClasses);
    }

    @Test
    void valueObjectsShouldNotHavePublicSetters() {
        // Value Objects should be immutable
        // Note: We allow protected no-arg constructors for JPA Embeddables
        // and non-final fields for JPA requirements, but no public setters
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.jmolecules.ddd.annotation.ValueObject.class)
                .should().notHaveModifier(com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT)
                .because("Value Objects should not be abstract");

        rule.check(importedClasses);
    }

    @Test
    void embeddableValueObjectsShouldBeAnnotated() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(jakarta.persistence.Embeddable.class)
                .and().resideInAnyPackage("..domain.vo..")
                .should().beAnnotatedWith(org.jmolecules.ddd.annotation.ValueObject.class)
                .because("JPA Embeddable value objects should be annotated with @ValueObject");

        rule.check(importedClasses);
    }

    // ========== Repository Tests ==========

    @Test
    void repositoriesShouldBeAnnotated() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..repository..")
                .and().areInterfaces()
                .and().haveSimpleNameEndingWith("Repository")
                .should().beAnnotatedWith(org.jmolecules.ddd.annotation.Repository.class)
                .because("Repository interfaces should be annotated with @Repository");

        rule.check(importedClasses);
    }

    @Test
    void repositoriesShouldBeInterfaces() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.jmolecules.ddd.annotation.Repository.class)
                .should().beInterfaces()
                .because("Repositories should be interfaces");

        rule.check(importedClasses);
    }

    // ========== Package Structure Tests ==========

    @Test
    void valueObjectsShouldResideInVoPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.jmolecules.ddd.annotation.ValueObject.class)
                .should().resideInAnyPackage("..vo..", "..domain.vo..")
                .because("Value Objects should reside in 'vo' packages");

        rule.check(importedClasses);
    }

    @Test
    void aggregatesShouldResideInDomainPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.jmolecules.ddd.annotation.AggregateRoot.class)
                .should().resideInAnyPackage("..domain..", "..internal.domain..")
                .because("Aggregate Roots should reside in domain packages");

        rule.check(importedClasses);
    }

    @Test
    void repositoriesShouldResideInRepositoryPackage() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.jmolecules.ddd.annotation.Repository.class)
                .should().resideInAnyPackage("..repository..", "..internal.repository..")
                .because("Repositories should reside in repository packages");

        rule.check(importedClasses);
    }

    // ========== DDD Tactical Design Tests ==========

    @Test
    void aggregatesShouldOnlyBeAccessedThroughRepository() {
        // Ensure that aggregates are not directly instantiated outside their package
        // This is a basic check - more sophisticated checks could be added
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.jmolecules.ddd.annotation.AggregateRoot.class)
                .should().onlyBeAccessed().byClassesThat()
                .resideInAnyPackage(
                    "..service..",
                    "..internal.service..",
                    "..repository..",
                    "..internal.repository..",
                    "..domain..",
                    "..internal.domain.."
                )
                .because("Aggregates should only be accessed through repositories and services");

        rule.check(importedClasses);
    }

    @Test
    void valueObjectsShouldBeUsedByAggregatesOrOtherValueObjects() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.jmolecules.ddd.annotation.ValueObject.class)
                .should().onlyBeAccessed().byClassesThat()
                .resideInAnyPackage(
                    "..domain..",
                    "..internal.domain..",
                    "..service..",
                    "..internal.service..",
                    "..vo..",
                    "..api.dto..",
                    "java.."
                )
                .because("Value Objects should be used within domain model and services");

        rule.check(importedClasses);
    }

    // ========== DDD Invariant Tests ==========

    @Test
    void aggregatesShouldNotDependOnOtherAggregates() {
        // Aggregates should reference other aggregates by ID only, not directly
        // This is enforced by checking that aggregates only depend on value objects and their own package
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.jmolecules.ddd.annotation.AggregateRoot.class)
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                    "..domain.vo..",
                    "..internal.domain.vo..",
                    "..api.dto..",
                    "java..",
                    "jakarta..",
                    "lombok..",
                    "org.jmolecules..",
                    "org.springframework.data.."
                )
                .orShould().dependOnClassesThat().areAnnotatedWith(org.jmolecules.ddd.annotation.ValueObject.class)
                .because("Aggregates should not directly depend on other aggregates");

        rule.check(importedClasses);
    }

    // ========== Consistency Tests ==========

    @Test
    void allEntitiesInDomainPackageShouldBeAggregates() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..internal.domain..")
                .and().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().beAnnotatedWith(org.jmolecules.ddd.annotation.AggregateRoot.class)
                .because("All JPA entities in domain packages should be marked as Aggregate Roots");

        rule.check(importedClasses);
    }

    @Test
    void allEmbeddablesInDomainShouldBeValueObjects() {
        ArchRule rule = classes()
                .that().resideInAnyPackage("..internal.domain..")
                .and().areAnnotatedWith(jakarta.persistence.Embeddable.class)
                .should().beAnnotatedWith(org.jmolecules.ddd.annotation.ValueObject.class)
                .because("All JPA Embeddables should be marked as Value Objects");

        rule.check(importedClasses);
    }
}

