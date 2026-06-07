package com.capitec.fraud.infrastructure.config;

import java.io.IOException;

import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.examples.Example;

class FraudOpenApiExampleLoader
{

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    FraudOpenApiExampleLoader(ResourceLoader resourceLoader, ObjectMapper objectMapper)
    {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }

    Example load(String resourceLocation)
    {
        var resource = resourceLoader.getResource(resourceLocation);

        try (var inputStream = resource.getInputStream())
        {
            return new Example().value(objectMapper.readTree(inputStream));
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Unable to load OpenAPI example: " + resourceLocation, ex);
        }
    }
}
