package com.humberto.b2b.ordermanagement.notification.infrastructure;

import com.humberto.b2b.ordermanagement.notification.outbox.NotificationPublisher;
import com.humberto.b2b.ordermanagement.notification.outbox.OutboxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.publisher", havingValue = "log", matchIfMissing = true)
class LoggingNotificationPublisher implements NotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationPublisher.class);

    @Override
    public void publish(OutboxMessage message) {
        log.info("Notification sent: eventType={} aggregateType={} aggregateId={} eventId={} payload={}",
                message.eventType(), message.aggregateType(), message.aggregateId(), message.eventId(),
                message.payload());
    }
}
