/*
 * Copyright (c) 2024 by Stefan Schubert under the MIT License (MIT).
 * See project LICENSE file for the detailed terms and conditions.
 */

package de.bluewhale.sabi;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;


/**
 * Checking on architectural constraints
 *
 * @author Stefan Schubert
 */
@ExtendWith(SpringExtension.class)
@Tag("DeveloperTests")
public class ArchitectureTest {

    private static final String PACKAGE_PREFIX = "de.bluewhale.sabi.";
    private static final String PACKAGE_PREFIX_WITH_WILDCARD = ".";

    private static JavaClasses classesFromSabi = new ClassFileImporter().importPackages(PACKAGE_PREFIX)
            .that(DescribedPredicate.not(JavaClass.Predicates.simpleNameEndingWith("Test")))
            .that(DescribedPredicate.not(JavaClass.Predicates.simpleNameEndingWith("TestSuite")));

    ArchCondition<JavaClass> isJavaClass = new ArchCondition<>("is a Java classes") {
        @Override
        public void check(JavaClass clazz, ConditionEvents events) {
            boolean satisfied = clazz.getPackageName().startsWith("java");
            String message = clazz.getDescription();
            events.add(new SimpleConditionEvent(clazz, satisfied, message));
        }
    };
    ArchCondition<JavaClass> isFrameworkClass = new ArchCondition<>("is a Framework Java class") {
        @Override
        public void check(JavaClass clazz, ConditionEvents events) {
            boolean satisfied = clazz.getPackageName().startsWith("org.") ||
                    clazz.getPackageName().startsWith("com.") ||
                    clazz.getPackageName().startsWith("io.swagger");
            String message = clazz.getDescription() + " is a dependency class.";
            events.add(new SimpleConditionEvent(clazz, satisfied, message));
        }
    };
    ArchCondition<JavaClass> isSabiClass = new ArchCondition<>("is any Sabi class") {
        @Override
        public void check(JavaClass clazz, ConditionEvents events) {
            boolean satisfied =  clazz.getPackageName().startsWith(PACKAGE_PREFIX);
            String message = clazz.getDescription() + " belongs to sabi backend core.";
            events.add(new SimpleConditionEvent(clazz, satisfied, message));
        }
    };

    ArchCondition<JavaClass> isSabiBoundaryClass = new ArchCondition<>("is any Boundary class") {
        @Override
        public void check(JavaClass clazz, ConditionEvents events) {
            boolean satisfied = clazz.getPackageName().startsWith("de.bluewhale.sabi.model") ||
                    clazz.getPackageName().startsWith("de.bluewhale.sabi.exception");
            String message = clazz.getDescription() + " belongs to sabi boundary layer.";
            events.add(new SimpleConditionEvent(clazz, satisfied, message));
        }
    };

    @Test
    public void test_onion_architecture_inside_one_component_using_layers() {
        // arrange
        Layer coreDataLayer = new Layer("JPA", PACKAGE_PREFIX_WITH_WILDCARD + ".persistence..");
        Layer serviceLayer = new Layer("Services", PACKAGE_PREFIX_WITH_WILDCARD + ".services..");
        Layer securityLayer = new Layer("Security", PACKAGE_PREFIX_WITH_WILDCARD + ".security..");
        Layer apiLayer = new Layer("API", PACKAGE_PREFIX_WITH_WILDCARD + ".rest..");
        Layer utilLayer = new Layer("Utilities", PACKAGE_PREFIX_WITH_WILDCARD + ".util..");

        Architectures.LayeredArchitecture layeredArchitecture = Architectures.layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer(coreDataLayer.name).definedBy(coreDataLayer.pkg)
                .layer(serviceLayer.name).definedBy(serviceLayer.pkg)
                .layer(securityLayer.name).definedBy(securityLayer.pkg)
                .layer(apiLayer.name).definedBy(apiLayer.pkg)
                .layer(utilLayer.name).definedBy(utilLayer.pkg);

        // act, assert
        layeredArchitecture
                .whereLayer(apiLayer.name).mayNotBeAccessedByAnyLayer()
                .whereLayer(apiLayer.name).mayOnlyAccessLayers(serviceLayer.name)
                .whereLayer(serviceLayer.name).mayOnlyAccessLayers(coreDataLayer.name, utilLayer.name, securityLayer.name);

        layeredArchitecture.evaluate(classesFromSabi);
    }

    class Layer {
        String name;
        String pkg;

        Layer(String name, String pkg) {
            this.name = name;
            this.pkg = pkg;
        }
    }

}
