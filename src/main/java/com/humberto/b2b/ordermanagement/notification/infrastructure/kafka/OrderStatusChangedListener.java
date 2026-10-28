package com.humberto.b2b.ordermanagement.notification.infrastructure.kafka;

import com.humberto.b2b.ordermanagement.notification.inbox.NotificationInbox;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "notification.publisher", havingValue = "kafka")
class OrderStatusChangedListener {

    private final NotificationInbox notificationInbox;

    OrderStatusChangedListener(NotificationInbox notificationInbox) {
        this.notificationInbox = notificationInbox;
    }

    @KafkaListener(topics = "${notification.kafka.topic}")
    void onMessage(ConsumerRecord<String, String> record) {
        Header eventId = record.headers().lastHeader(KafkaNotificationPublisher.EVENT_ID_HEADER);
        if (eventId == null || record.key() == null) {
            throw new IllegalArgumentException("Message without eventId header or key at offset " + record.offset());
        }
        notificationInbox.receive(
                UUID.fromString(new String(eventId.value(), StandardCharsets.UTF_8)),
                UUID.fromString(record.key()),
                record.value());
    }
}
