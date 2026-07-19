package com.marceloituccayasi.ocv;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.springframework.data.repository.Repository;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.persistence.Entity;

@AnalyzeClasses(packages = "com.marceloituccayasi.ocv")
class PersistenceArchitectureTest {

    private static final String OPERATIONAL_CLOSE_ENTITY =
            "..operationalclose.infrastructure.persistence.entity..";
    private static final String OPERATIONAL_CLOSE_REPOSITORY =
            "..operationalclose.infrastructure.persistence.repository..";
    private static final String OPERATIONAL_CLOSE_PERSISTENCE =
            "..operationalclose.infrastructure.persistence..";

    private static final String IDENTITY_ACCESS_ENTITY =
            "..identityaccess.infrastructure.persistence.entity..";
    private static final String IDENTITY_ACCESS_REPOSITORY =
            "..identityaccess.infrastructure.persistence.repository..";
    private static final String IDENTITY_ACCESS_PERSISTENCE =
            "..identityaccess.infrastructure.persistence..";

    @ArchTest
    static final ArchRule jpaEntitiesMustResideInDedicatedEntityPackages =
            classes()
                    .that()
                    .areAnnotatedWith(Entity.class)
                    .should()
                    .resideInAnyPackage(
                            OPERATIONAL_CLOSE_ENTITY,
                            IDENTITY_ACCESS_ENTITY)
                    .because("JPA entities are infrastructure persistence models")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule springDataRepositoriesMustResideInRepositoryPackages =
            classes()
                    .that()
                    .areAssignableTo(Repository.class)
                    .should()
                    .resideInAnyPackage(
                            OPERATIONAL_CLOSE_REPOSITORY,
                            IDENTITY_ACCESS_REPOSITORY)
                    .because("Spring Data repositories are infrastructure details")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistenceEntitiesMustNotDependOnDomainObjects =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            OPERATIONAL_CLOSE_ENTITY,
                            IDENTITY_ACCESS_ENTITY)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..domain..")
                    .because("JPA and domain models must be mapped explicitly")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule innerLayersMustNotDependOnPersistenceImplementation =
            noClasses()
                    .that()
                    .resideInAnyPackage(
                            "..domain..",
                            "..application..",
                            "..presentation..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            OPERATIONAL_CLOSE_PERSISTENCE,
                            IDENTITY_ACCESS_PERSISTENCE,
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "org.springframework.data..")
                    .because("persistence implementation must remain behind application ports");

}
