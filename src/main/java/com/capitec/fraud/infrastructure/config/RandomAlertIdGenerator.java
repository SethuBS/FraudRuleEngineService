package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.application.AlertIdGenerator;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class RandomAlertIdGenerator implements AlertIdGenerator
{

    @Override
    public UUID nextAlertId()
    {
        return UUID.randomUUID();
    }
}
