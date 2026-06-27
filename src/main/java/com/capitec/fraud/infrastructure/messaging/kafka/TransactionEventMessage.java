package com.capitec.fraud.infrastructure.messaging.kafka;

import com.capitec.fraud.application.TransactionEvaluationCommand;

public record TransactionEventMessage(
        TransactionEvaluationCommand command,
        String correlationId,
        String eventId,
        String eventType,
        String schemaVersion)
{
}
