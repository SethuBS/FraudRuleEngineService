package com.capitec.fraud.application;

public class TransactionEvaluationException extends RuntimeException
{

    public TransactionEvaluationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
