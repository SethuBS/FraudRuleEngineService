package com.capitec.fraud.infrastructure.messaging.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
@ConditionalOnProperty(prefix = "fraud.kafka", name = "enabled", havingValue = "true")
public class KafkaTransactionEventMetrics
{

    private final Counter consumedEvents;
    private final Counter evaluatedEvents;
    private final Counter duplicateEvents;
    private final Counter failedEvents;
    private final Counter deadLetteredEvents;

    public KafkaTransactionEventMetrics(MeterRegistry meterRegistry)
    {
        this.consumedEvents = counter(meterRegistry, "fraud.kafka.transaction.events.consumed");
        this.evaluatedEvents = counter(meterRegistry, "fraud.kafka.transaction.events.evaluated");
        this.duplicateEvents = counter(meterRegistry, "fraud.kafka.transaction.events.duplicates");
        this.failedEvents = counter(meterRegistry, "fraud.kafka.transaction.events.failed");
        this.deadLetteredEvents = counter(meterRegistry, "fraud.kafka.transaction.events.deadlettered");
    }

    public void recordConsumedEvent()
    {
        consumedEvents.increment();
    }

    public void recordEvaluatedEvent()
    {
        evaluatedEvents.increment();
    }

    public void recordDuplicateEvent()
    {
        duplicateEvents.increment();
    }

    public void recordFailedEvent()
    {
        failedEvents.increment();
    }

    public void recordDeadLetteredEvent()
    {
        deadLetteredEvents.increment();
    }

    private static Counter counter(MeterRegistry meterRegistry, String name)
    {
        return Counter.builder(name)
                .description("Fraud Rule Engine Kafka transaction-event adapter metric.")
                .register(meterRegistry);
    }
}
