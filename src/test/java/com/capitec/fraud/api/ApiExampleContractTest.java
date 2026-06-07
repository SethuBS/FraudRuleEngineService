package com.capitec.fraud.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.capitec.fraud.api.dto.TransactionEvaluationRequest;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.json.JsonMapper;

class ApiExampleContractTest
{

    private final JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @ParameterizedTest
    @ValueSource(strings = {
        "examples/high-risk-transaction.json",
        "examples/low-risk-transaction.json",
        "examples/duplicate-event.json"
    })
    void transactionRequestExamplesMatchTheHttpContract(String examplePath)
            throws Exception
    {
        var request = objectMapper.readValue(Files.readString(Path.of(examplePath)), TransactionEvaluationRequest.class);

        assertThat(request.eventId()).isNotBlank();
        assertThat(request.transactionId()).isNotBlank();
        assertThat(request.customerId()).isNotBlank();
        assertThat(request.accountId()).isNotBlank();
        assertThat(request.amount()).isPositive();
        assertThat(request.currency()).isNotBlank();
        assertThat(request.transactionTimestamp()).isNotNull();
        assertThat(request.merchantCategory()).isNotBlank();
        assertThatCode(request::toDomain).doesNotThrowAnyException();
    }
}
