package com.capitec.fraud.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ControllerPersistenceBoundaryTest
{

    @Test
    void apiLayerDoesNotDependOnPersistenceEntities()
            throws Exception
    {
        var apiSourceRoot = Path.of("src/main/java/com/capitec/fraud/api");
        try (var javaFiles = Files.walk(apiSourceRoot))
        {
            var filesWithPersistenceImports = javaFiles
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> sourceContainsPersistencePackage(path))
                    .toList();

            assertThat(filesWithPersistenceImports).isEmpty();
        }
    }

    private static boolean sourceContainsPersistencePackage(Path path)
    {
        try
        {
            return Files.readString(path).contains("com.capitec.fraud.infrastructure.persistence");
        } catch (java.io.IOException ex)
        {
            throw new IllegalStateException("Unable to read source file " + path, ex);
        }
    }
}
