package com.humberto.b2b.ordermanagement.notification.infrastructure.kafka;

import com.humberto.b2b.ordermanagement.notification.outbox.NotificationPublisher;
import com.humberto.b2b.ordermanagement.notification.outbox.OutboxMessage;
import com.humberto.b2b.ordermanagement.notification.outbox.PartialPublishException;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnProperty(name = "notification.publisher", havingValue = "kafka")
class KafkaNotificationPublisher implements NotificationPublisher {

    static final String EVENT_ID_HEADER = "eventId";
    static final String EVENT_TYPE_HEADER = "eventType";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final Duration sendTimeout;

    KafkaNotificationPublisher(KafkaTemplate<String, String> kafkaTemplate,
                               @Value("${notification.kafka.topic}") String topic,
                               @Value("${notification.kafka.send-timeout}") Duration sendTimeout) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.sendTimeout = sendTimeout;
    }

    @Override
    public void publish(OutboxMessage message) {
        publishAll(List.of(message));
    }

    @Override
    public int publishAll(List<OutboxMessage> messages) {
        List<CompletableFuture<SendResult<String, String>>> sends = messages.stream()
                .map(message -> kafkaTemplate.send(recordOf(message)))
                .toList();
        long deadline = System.nanoTime() + sendTimeout.toNanos();
        for (int i = 0; i < sends.size(); i++) {
            try {
                sends.get(i).get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new PartialPublishException(i, exception);
            } catch (ExecutionException | TimeoutException exception) {
                throw new PartialPublishException(i, exception);
            }
        }
        return sends.size();
    }

    private ProducerRecord<String, String> recordOf(OutboxMessage message) {
        ProducerRecord<String, String> record =
                new ProducerRecord<>(topic, message.aggregateId().toString(), message.payload());
        record.headers().add(EVENT_ID_HEADER, message.eventId().toString().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EVENT_TYPE_HEADER, message.eventType().getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
