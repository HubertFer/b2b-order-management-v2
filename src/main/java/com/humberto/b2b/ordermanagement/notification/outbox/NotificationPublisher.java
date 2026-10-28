package com.humberto.b2b.ordermanagement.notification.outbox;

public interface NotificationPublisher {

    void publish(OutboxMessage message);
}
