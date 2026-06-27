package com.capitec.fraud.infrastructure.messaging.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.kafka.common.header.Headers;

public final class KafkaRecordHeaders
{

    private KafkaRecordHeaders()
    {
    }

    public static Optional<String> optionalHeader(Headers headers, String name)
    {
        var header = headers.lastHeader(name);
        if (header == null || header.value() == null || header.value().length == 0)
        {
            return Optional.empty();
        }

        return Optional.of(new String(header.value(), StandardCharsets.UTF_8));
    }
}
