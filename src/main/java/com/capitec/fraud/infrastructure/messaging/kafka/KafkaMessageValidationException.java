package com.capitec.fraud.infrastructure.messaging.kafka;

public class KafkaMessageValidationException extends RuntimeException
{

    public KafkaMessageValidationException(String message)
    {
        super(message);
    }

    public KafkaMessageValidationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
