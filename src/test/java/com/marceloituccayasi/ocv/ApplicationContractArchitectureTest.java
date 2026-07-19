package com.marceloituccayasi.ocv;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.marceloituccayasi.ocv")
class ApplicationContractArchitectureTest {

    private static final String APPLICATION_REPOSITORY_PORTS =
            "..application.port.repository..";

    private static final String APPLICATION_PACKAGES =
            "..application..";

    @ArchTest
    static final ArchRule repositoryPortsMustBeInterfaces =
            classes()
                    .that()
                    .resideInAPackage(APPLICATION_REPOSITORY_PORTS)
                    .should()
                    .beInterfaces()
                    .because("repository ports are application contracts implemented by infrastructure")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule applicationRepositoriesMustUseTheRepositoryPortPackage =
            classes()
                    .that()
                    .resideInAPackage(APPLICATION_PACKAGES)
                    .and()
                    .haveSimpleNameEndingWith("Repository")
                    .should()
                    .resideInAPackage(APPLICATION_REPOSITORY_PORTS)
                    .because("application repository contracts must have one explicit location")
                    .allowEmptyShould(true);

}
