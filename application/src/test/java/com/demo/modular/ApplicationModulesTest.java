package com.demo.modular;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Test class for verifying Spring Modulith module structure and generating documentation
 */
class ApplicationModulesTest {

    ApplicationModules modules = ApplicationModules.of(ModularMonolithApplication.class);

    @Test
    void verifyModuleStructure() {
        // Verifies that module boundaries are not violated
        // This test will fail if any module accesses internals of another module
        // Note: Service implementations in internal packages can access public APIs of other modules
        modules.verify();
    }

    @Test
    void writeDocumentation() throws Exception {
        // Generates PlantUML diagrams showing module structure and dependencies
        new Documenter(modules)
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml();
    }

    @Test
    void writeModuleCanvas() throws Exception {
        // Generates module canvas documentation
        new Documenter(modules)
            .writeModuleCanvases();
    }
}

