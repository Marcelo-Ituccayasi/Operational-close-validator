package com.marceloituccayasi.ocv;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.marceloituccayasi.ocv")
class BootstrapArchitectureTest {

    @ArchTest
    static final ArchRule classesMustRemainInsideTheOcvBasePackage =
            classes()
                    .should()
                    .resideInAPackage("com.marceloituccayasi.ocv..");

}
