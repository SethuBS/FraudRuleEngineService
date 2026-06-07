package com.capitec.fraud.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitec.fraud.api.OpenApiOperationIds;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

class FraudOpenApiConfigurationTest
{

    private static final String APPLICATION_JSON_MEDIA_TYPE = "application/json";
    private static final String SUCCESS_RESPONSE_CODE = "200";
    private static final String BAD_REQUEST_RESPONSE_CODE = "400";
    private static final String NOT_FOUND_RESPONSE_CODE = "404";

    private final FraudOpenApiProperties properties = new FraudOpenApiProperties(
            "Fraud Rule Engine Service API",
            "Evaluates fraud rule decisions",
            "test-version",
            "bearer-jwt",
            "bearer",
            "JWT",
            new FraudOpenApiProperties.Examples(
                    "classpath:openapi/examples/high-risk-transaction.json",
                    "classpath:openapi/examples/low-risk-transaction.json",
                    "classpath:openapi/examples/duplicate-event.json",
                    "classpath:openapi/examples/high-risk-evaluation-response.json",
                    "classpath:openapi/examples/low-risk-evaluation-response.json",
                    "classpath:openapi/examples/fraud-alert-list-response.json",
                    "classpath:openapi/examples/fraud-alert-response.json",
                    "classpath:openapi/examples/validation-error-response.json",
                    "classpath:openapi/examples/not-found-response.json"));

    @Test
    void openApiMetadataAndJwtBearerSecurityAreConfigured()
    {
        var openApi = new FraudOpenApiConfiguration().fraudRuleEngineOpenApi(properties);

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Fraud Rule Engine Service API");
        assertThat(openApi.getInfo().getDescription()).isEqualTo("Evaluates fraud rule decisions");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("test-version");
        assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearer-jwt");
        assertThat(openApi.getComponents().getSecuritySchemes().get("bearer-jwt").getScheme()).isEqualTo("bearer");
        assertThat(openApi.getComponents().getSecuritySchemes().get("bearer-jwt").getBearerFormat()).isEqualTo("JWT");
        assertThat(openApi.getSecurity()).singleElement().satisfies(securityRequirement ->
                assertThat(securityRequirement).containsKey("bearer-jwt"));
    }

    @Test
    void openApiExamplesAreLoadedFromConfiguredResources()
    {
        var openApi = openApiWithDocumentedOperations();
        var customizer = new FraudOpenApiConfiguration().fraudRuleEngineExamplesOpenApiCustomizer(
                properties,
                new DefaultResourceLoader(),
                JsonMapper.builder().findAndAddModules().build());

        customizer.customise(openApi);

        var evaluationOperation = operation(openApi, OpenApiOperationIds.EVALUATE_TRANSACTION);
        assertThat(jsonMediaType(evaluationOperation.getRequestBody().getContent()).getExamples())
                .containsKeys("High risk transaction", "Low risk transaction", "Duplicate event");
        assertThat(jsonMediaType(evaluationOperation.getResponses().get(SUCCESS_RESPONSE_CODE).getContent()).getExamples())
                .containsKeys("High risk response", "Low risk response");
        assertThat(jsonMediaType(evaluationOperation.getResponses().get(BAD_REQUEST_RESPONSE_CODE).getContent()).getExamples())
                .containsKey("Validation error");

        var alertsOperation = operation(openApi, OpenApiOperationIds.LIST_FRAUD_ALERTS);
        assertThat(jsonMediaType(alertsOperation.getResponses().get(SUCCESS_RESPONSE_CODE).getContent()).getExamples())
                .containsKey("Fraud alert page");

        var alertDetailOperation = operation(openApi, OpenApiOperationIds.GET_FRAUD_ALERT);
        assertThat(jsonMediaType(alertDetailOperation.getResponses().get(NOT_FOUND_RESPONSE_CODE).getContent()).getExamples())
                .containsKey("Missing fraud alert");

        var evaluationLookupOperation = operation(openApi, OpenApiOperationIds.GET_TRANSACTION_FRAUD_EVALUATION);
        assertThat(jsonMediaType(evaluationLookupOperation.getResponses().get(NOT_FOUND_RESPONSE_CODE).getContent()).getExamples())
                .containsKey("Missing transaction evaluation");
    }

    @Test
    void productionProfileDisablesSwaggerExposure()
    {
        var yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(new FileSystemResource("src/main/resources/application-prod.yml"));

        var properties = yamlPropertiesFactoryBean.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
    }

    private static OpenAPI openApiWithDocumentedOperations()
    {
        return new OpenAPI().paths(new Paths()
                .addPathItem(
                        "/api/v1/transactions/evaluate",
                        new PathItem().post(operationWithRequestBody(OpenApiOperationIds.EVALUATE_TRANSACTION)))
                .addPathItem(
                        "/api/v1/fraud-alerts",
                        new PathItem().get(operation(OpenApiOperationIds.LIST_FRAUD_ALERTS)))
                .addPathItem(
                        "/api/v1/fraud-alerts/{alertId}",
                        new PathItem().get(operation(OpenApiOperationIds.GET_FRAUD_ALERT)))
                .addPathItem(
                        "/api/v1/transactions/{transactionId}/fraud-evaluation",
                        new PathItem().get(operation(OpenApiOperationIds.GET_TRANSACTION_FRAUD_EVALUATION))));
    }

    private static Operation operationWithRequestBody(String operationId)
    {
        return operation(operationId).requestBody(new RequestBody().content(jsonContent()));
    }

    private static Operation operation(String operationId)
    {
        return new Operation()
                .operationId(operationId)
                .responses(new ApiResponses()
                        .addApiResponse(SUCCESS_RESPONSE_CODE, apiResponse())
                        .addApiResponse(BAD_REQUEST_RESPONSE_CODE, apiResponse())
                        .addApiResponse(NOT_FOUND_RESPONSE_CODE, apiResponse()));
    }

    private static ApiResponse apiResponse()
    {
        return new ApiResponse().content(jsonContent());
    }

    private static Content jsonContent()
    {
        return new Content().addMediaType(APPLICATION_JSON_MEDIA_TYPE, new MediaType());
    }

    private static MediaType jsonMediaType(Content content)
    {
        return content.get(APPLICATION_JSON_MEDIA_TYPE);
    }

    private static Operation operation(OpenAPI openApi, String operationId)
    {
        return openApi.getPaths()
                .values()
                .stream()
                .flatMap(pathItem -> pathItem.readOperations().stream())
                .filter(operation -> operationId.equals(operation.getOperationId()))
                .findFirst()
                .orElseThrow();
    }
}
