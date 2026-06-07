package com.capitec.fraud.api;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException
{

    public ResourceNotFoundException(String message)
    {
        super(message);
    }

    public static ResourceNotFoundException fraudAlert(UUID alertId)
    {
        return new ResourceNotFoundException("Fraud alert was not found: " + alertId);
    }

    public static ResourceNotFoundException transactionEvaluation(String transactionId)
    {
        return new ResourceNotFoundException("Fraud evaluation was not found for transaction: " + transactionId);
    }
}
