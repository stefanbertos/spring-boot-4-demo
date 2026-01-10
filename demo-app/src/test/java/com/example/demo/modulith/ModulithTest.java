package com.example.demo.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith tests to verify modular structure.
 */
class ModulithTest {

    private final ApplicationModules modules = ApplicationModules.of("com.example.demo");

    @Test
    void shouldBeCompliant() {
        modules.verify();
    }

    @Test
    void shouldWriteDocumentation() {
        new Documenter(modules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }
}
