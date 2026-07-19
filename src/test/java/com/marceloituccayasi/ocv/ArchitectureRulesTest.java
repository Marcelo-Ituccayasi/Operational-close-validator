package com.marceloituccayasi.ocv;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.marceloituccayasi.ocv")
class ArchitectureRulesTest {

    private static final String OPERATIONAL_CLOSE =
            "..operationalclose..";
    private static final String OPERATIONAL_CLOSE_DOMAIN =
            "..operationalclose.domain..";
    private static final String OPERATIONAL_CLOSE_APPLICATION =
            "..operationalclose.application..";
    private static final String OPERATIONAL_CLOSE_APPLICATION_PORT =
            "..operationalclose.application.port..";
    private static final String OPERATIONAL_CLOSE_PRESENTATION =
            "..operationalclose.presentation..";
    private static final String OPERATIONAL_CLOSE_INFRASTRUCTURE =
            "..operationalclose.infrastructure..";

    private static final String IDENTITY_ACCESS =
            "..identityaccess..";
    private static final String IDENTITY_ACCESS_APPLICATION =
            "..identityaccess.application..";
    private static final String IDENTITY_ACCESS_PRESENTATION =
            "..identityaccess.presentation..";
    private static final String IDENTITY_ACCESS_INFRASTRUCTURE =
            "..identityaccess.infrastructure..";

    @ArchTest
    static final ArchRule operationalCloseDomainMustRemainFrameworkIndependent =
            noClasses()
                    .that()
                    .resideInAPackage(OPERATIONAL_CLOSE_DOMAIN)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.hibernate..")
                    .because("the domain must remain independent of frameworks")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule operationalCloseDomainMustNotDependOutward =
            noClasses()
                    .that()
                    .resideInAPackage(OPERATIONAL_CLOSE_DOMAIN)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            OPERATIONAL_CLOSE_APPLICATION,
                            OPERATIONAL_CLOSE_PRESENTATION,
                            OPERATIONAL_CLOSE_INFRASTRUCTURE,
                            IDENTITY_ACCESS)
                    .because("dependencies must point toward the domain")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule operationalCloseApplicationMustRemainInsideItsBoundary =
            noClasses()
                    .that()
                    .resideInAPackage(OPERATIONAL_CLOSE_APPLICATION)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            OPERATIONAL_CLOSE_PRESENTATION,
                            OPERATIONAL_CLOSE_INFRASTRUCTURE,
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "org.springframework.data..",
                            "org.springframework.web..",
                            "org.springframework.security..",
                            "org.thymeleaf..")
                    .because("application code must not depend on adapters or persistence frameworks");

    @ArchTest
    static final ArchRule operationalCloseApplicationPortsMustBeInterfaces =
            classes()
                    .that()
                    .resideInAPackage(OPERATIONAL_CLOSE_APPLICATION_PORT)
                    .should()
                    .beInterfaces()
                    .because("output ports must remain implementation-independent contracts");

    @ArchTest
    static final ArchRule operationalClosePresentationMustNotBypassApplication =
            noClasses()
                    .that()
                    .resideInAPackage(OPERATIONAL_CLOSE_PRESENTATION)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            OPERATIONAL_CLOSE_DOMAIN,
                            OPERATIONAL_CLOSE_APPLICATION_PORT,
                            OPERATIONAL_CLOSE_INFRASTRUCTURE,
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "org.springframework.data..")
                    .because("presentation must delegate through application use cases")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule operationalCloseInfrastructureMustNotDependOnPresentation =
            noClasses()
                    .that()
                    .resideInAPackage(OPERATIONAL_CLOSE_INFRASTRUCTURE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(OPERATIONAL_CLOSE_PRESENTATION)
                    .because("output adapters must not depend on input adapters");

    @ArchTest
    static final ArchRule identityAccessApplicationMustRemainFrameworkIndependent =
            noClasses()
                    .that()
                    .resideInAPackage(IDENTITY_ACCESS_APPLICATION)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            IDENTITY_ACCESS_PRESENTATION,
                            IDENTITY_ACCESS_INFRASTRUCTURE,
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.hibernate..")
                    .because("identity application contracts must remain framework-independent");

    @ArchTest
    static final ArchRule identityAccessMustNotDependOnOperationalCloseManagement =
            noClasses()
                    .that()
                    .resideInAPackage(IDENTITY_ACCESS)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(OPERATIONAL_CLOSE)
                    .because("identity and access is an independent support module");

    @ArchTest
    static final ArchRule operationalCloseMustNotUseIdentityImplementationDetails =
            noClasses()
                    .that()
                    .resideInAPackage(OPERATIONAL_CLOSE)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            IDENTITY_ACCESS_PRESENTATION,
                            IDENTITY_ACCESS_INFRASTRUCTURE)
                    .because("operational close management may use only identity application contracts");

    @ArchTest
    static final ArchRule businessModulesMustBeFreeOfCycles =
            slices()
                    .matching("com.marceloituccayasi.ocv.(*)..")
                    .should()
                    .beFreeOfCycles()
                    .because("the modular monolith must preserve acyclic module dependencies");

}
