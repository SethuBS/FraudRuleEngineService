package com.capitec.fraud.infrastructure.config;

import com.capitec.fraud.infrastructure.messaging.kafka.KafkaMessageValidationException;
import com.capitec.fraud.infrastructure.messaging.kafka.KafkaTransactionEventMetrics;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@ConditionalOnProperty(prefix = "fraud.kafka", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(FraudKafkaProperties.class)
public class FraudKafkaConfiguration
{

    private static final Logger LOG = LoggerFactory.getLogger(FraudKafkaConfiguration.class);

    @Bean
    NewTopics fraudKafkaTopics(FraudKafkaProperties properties)
    {
        return new NewTopics(
                TopicBuilder.name(properties.transactionEventsTopic())
                        .partitions(properties.topics().partitions())
                        .replicas(properties.topics().replicationFactor())
                        .build(),
                TopicBuilder.name(properties.deadLetterTopic())
                        .partitions(properties.topics().partitions())
                        .replicas(properties.topics().replicationFactor())
                        .build());
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> transactionEventKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<Object, Object> kafkaTemplate,
            FraudKafkaProperties properties,
            KafkaTransactionEventMetrics metrics)
    {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(transactionEventErrorHandler(kafkaTemplate, properties, metrics));

        return factory;
    }

    private DefaultErrorHandler transactionEventErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate,
            FraudKafkaProperties properties,
            KafkaTransactionEventMetrics metrics)
    {
        var recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> deadLetterDestination(record, exception, properties, metrics));
        var retry = properties.retry();
        var errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retry.backoff().toMillis(), retry.maxAttempts() - 1L));
        errorHandler.addNotRetryableExceptions(KafkaMessageValidationException.class);
        errorHandler.setRetryListeners((record, exception, deliveryAttempt) -> LOG.warn(
                "event=kafka_transaction_event_retry topic={} partition={} offset={} deliveryAttempt={} reason={}",
                record.topic(),
                record.partition(),
                record.offset(),
                deliveryAttempt,
                exception.getClass().getSimpleName()));

        return errorHandler;
    }

    private TopicPartition deadLetterDestination(
            ConsumerRecord<?, ?> record,
            Exception exception,
            FraudKafkaProperties properties,
            KafkaTransactionEventMetrics metrics)
    {
        metrics.recordDeadLetteredEvent();
        LOG.warn(
                "event=kafka_transaction_event_dead_lettered sourceTopic={} partition={} offset={} eventId={} "
                        + "deadLetterTopic={} reason={}",
                record.topic(),
                record.partition(),
                record.offset(),
                optionalHeader(record, properties.headers().eventId()).orElse("unknown"),
                properties.deadLetterTopic(),
                exception.getClass().getSimpleName());

        return new TopicPartition(properties.deadLetterTopic(), record.partition());
    }

    private static Optional<String> optionalHeader(ConsumerRecord<?, ?> record, String name)
    {
        var header = record.headers().lastHeader(name);
        if (header == null || header.value() == null || header.value().length == 0)
        {
            return Optional.empty();
        }

        return Optional.of(new String(header.value(), StandardCharsets.UTF_8));
    }
}
