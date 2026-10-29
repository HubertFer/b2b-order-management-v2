package com.humberto.b2b.ordermanagement.notification.outbox;

import java.util.List;

public interface NotificationPublisher {

    void publish(OutboxMessage message);

    default int publishAll(List<OutboxMessage> messages) {
        for (int i = 0; i < messages.size(); i++) {
            try {
                publish(messages.get(i));
            } catch (RuntimeException exception) {
                throw new PartialPublishException(i, exception);
            }
        }
        return messages.size();
    }
}
