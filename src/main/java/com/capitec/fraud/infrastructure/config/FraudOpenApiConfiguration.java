package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.api.OpenApiOperationIds;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
@EnableConfigurationProperties(FraudOpenApiProperties.class)
public class FraudOpenApiConfiguration
{

    private static final String APPLICATION_JSON_MEDIA_TYPE = "application/json";
    private static final String SUCCESS_RESPONSE_CODE = "200";
    private static final String BAD_REQUEST_RESPONSE_CODE = "400";
    private static final String NOT_FOUND_RESPONSE_CODE = "404";

    @Bean
    OpenAPI fraudRuleEngineOpenApi(FraudOpenApiProperties properties)
    {
        var securitySchemeName = properties.bearerSecuritySchemeName();

        return new OpenAPI()
                .info(new Info()
                        .title(properties.title())
                        .description(properties.description())
                        .version(properties.version()))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme(properties.bearerScheme())
                                .bearerFormat(properties.bearerFormat())))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }

    @Bean
    OpenApiCustomizer fraudRuleEngineExamplesOpenApiCustomizer(
            FraudOpenApiProperties properties,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper)
    {
        var exampleLoader = new FraudOpenApiExampleLoader(resourceLoader, objectMapper);

        return openApi ->
        {
            addRequestExample(
                    openApi,
                    OpenApiOperationIds.EVALUATE_TRANSACTION,
                    "High risk transaction",
                    exampleLoader.load(properties.examples().highRiskTransaction()));
            addRequestExample(
                    openApi,
                    OpenApiOperationIds.EVALUATE_TRANSACTION,
                    "Low risk transaction",
                    exampleLoader.load(properties.examples().lowRiskTransaction()));
            addRequestExample(
                    openApi,
                    OpenApiOperationIds.EVALUATE_TRANSACTION,
                    "Duplicate event",
                    exampleLoader.load(properties.examples().duplicateEvent()));
            addResponseExample(
                    openApi,
                    OpenApiOperationIds.EVALUATE_TRANSACTION,
                    SUCCESS_RESPONSE_CODE,
                    "High risk response",
                    exampleLoader.load(properties.examples().highRiskResponse()));
            addResponseExample(
                    openApi,
                    OpenApiOperationIds.EVALUATE_TRANSACTION,
                    SUCCESS_RESPONSE_CODE,
                    "Low risk response",
                    exampleLoader.load(properties.examples().lowRiskResponse()));
            addResponseExample(
                    openApi,
                    OpenApiOperationIds.EVALUATE_TRANSACTION,
                    BAD_REQUEST_RESPONSE_CODE,
                    "Validation error",
                    exampleLoader.load(properties.examples().validationErrorResponse()));
            addResponseExample(
                    openApi,
                    OpenApiOperationIds.LIST_FRAUD_ALERTS,
                    SUCCESS_RESPONSE_CODE,
                    "Fraud alert page",
                    exampleLoader.load(properties.examples().alertListResponse()));
            addResponseExample(
                    openApi,
                    OpenApiOperationIds.LIST_FRAUD_ALERTS,
                    BAD_REQUEST_RESPONSE_CODE,
                    "Validation error",
                    exampleLoader.load(properties.examples().validationErrorResponse()));
            addResponseExample(
                    openApi,
                    OpenApiOperationIds.GET_FRAUD_ALERT,
                    SUCCESS_RESPONSE_CODE,
                    "Fraud alert",
                    exampleLoader.load(properties.examples().alertResponse()));
            addResponseExample(
                    openApi,
                    OpenApiOperationIds.GET_FRAUD_ALERT,
                    NOT_FOUND_RESPONSE_CODE,
                    "Missing fraud alert",
                    exampleLoader.load(properties.examples().notFoundResponse()));
            addResponseExample(
                    openApi,
                    OpenApiOperationIds.GET_TRANSACTION_FRAUD_EVALUATION,
                    SUCCESS_RESPONSE_CODE,
                    "Stored transaction evaluation",
                    exampleLoader.load(properties.examples().highRiskResponse()));
            addResponseExample(
                    openApi,
                    OpenApiOperationIds.GET_TRANSACTION_FRAUD_EVALUATION,
                    NOT_FOUND_RESPONSE_CODE,
                    "Missing transaction evaluation",
                    exampleLoader.load(properties.examples().notFoundResponse()));
        };
    }

    private static void addRequestExample(
            OpenAPI openApi,
            String operationId,
            String name,
            Example example)
    {
        operations(openApi)
                .filter(operation -> operationId.equals(operation.getOperationId()))
                .forEach(operation -> mediaType(operation.getRequestBody().getContent().get(APPLICATION_JSON_MEDIA_TYPE))
                        .addExamples(name, example));
    }

    private static void addResponseExample(
            OpenAPI openApi,
            String operationId,
            String responseCode,
            String name,
            Example example)
    {
        operations(openApi)
                .filter(operation -> operationId.equals(operation.getOperationId()))
                .forEach(operation -> mediaType(
                        operation.getResponses()
                                .get(responseCode)
                                .getContent()
                                .get(APPLICATION_JSON_MEDIA_TYPE))
                        .addExamples(name, example));
    }

    private static java.util.stream.Stream<io.swagger.v3.oas.models.Operation> operations(OpenAPI openApi)
    {
        if (openApi.getPaths() == null)
        {
            return java.util.stream.Stream.empty();
        }

        return openApi.getPaths()
                .values()
                .stream()
                .flatMap(pathItem -> pathItem.readOperations().stream());
    }

    private static MediaType mediaType(MediaType mediaType)
    {
        if (mediaType == null)
        {
            throw new IllegalStateException("OpenAPI operation is missing application/json media type");
        }

        return mediaType;
    }
}
