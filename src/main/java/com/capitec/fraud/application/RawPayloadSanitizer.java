package com.capitec.fraud.application;

import com.capitec.fraud.infrastructure.config.FraudRawPayloadProperties;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

@Component
public class RawPayloadSanitizer
{

    private final ObjectMapper objectMapper;
    private final Set<String> sensitiveFieldNames;
    private final String redactedValue;

    public RawPayloadSanitizer(
            ObjectMapper objectMapper,
            FraudRawPayloadProperties properties)
    {
        this.objectMapper = objectMapper;
        this.sensitiveFieldNames = normalizedFields(properties.sensitiveFieldNames());
        this.redactedValue = properties.redactedValue();
    }

    public String sanitize(String rawPayload)
    {
        try
        {
            var sanitizedPayload = objectMapper.readTree(rawPayload);
            sanitizeNode(sanitizedPayload);

            return objectMapper.writeValueAsString(sanitizedPayload);
        }
        catch (JsonProcessingException ex)
        {
            throw new IllegalArgumentException("Request body must be valid JSON", ex);
        }
    }

    private void sanitizeNode(JsonNode node)
    {
        if (node.isObject())
        {
            sanitizeObject((ObjectNode) node);
        }
        else if (node.isArray())
        {
            node.forEach(this::sanitizeNode);
        }
    }

    private void sanitizeObject(ObjectNode objectNode)
    {
        var fieldNames = objectNode.fieldNames();
        while (fieldNames.hasNext())
        {
            var fieldName = fieldNames.next();
            if (sensitiveFieldNames.contains(normalizedField(fieldName)))
            {
                objectNode.set(fieldName, TextNode.valueOf(redactedValue));
            }
            else
            {
                sanitizeNode(objectNode.get(fieldName));
            }
        }
    }

    private static Set<String> normalizedFields(Iterable<String> fieldNames)
    {
        var normalizedFields = new HashSet<String>();
        fieldNames.forEach(fieldName -> normalizedFields.add(normalizedField(fieldName)));

        return Set.copyOf(normalizedFields);
    }

    private static String normalizedField(String fieldName)
    {
        return fieldName.toLowerCase(Locale.ROOT);
    }
}
