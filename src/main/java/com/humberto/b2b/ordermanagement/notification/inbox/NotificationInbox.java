package com.humberto.b2b.ordermanagement.notification.inbox;

import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public class NotificationInbox {

    private final ProcessedEventStore processedEventStore;
    private final PartnerNotifier partnerNotifier;
    private final Clock clock;

    public NotificationInbox(ProcessedEventStore processedEventStore, PartnerNotifier partnerNotifier, Clock clock) {
        this.processedEventStore = processedEventStore;
        this.partnerNotifier = partnerNotifier;
        this.clock = clock;
    }

    @Transactional
    public boolean receive(UUID eventId, UUID orderId, String payload) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        if (!processedEventStore.markProcessed(eventId, clock.instant())) {
            return false;
        }
        partnerNotifier.notifyOrderStatusChanged(orderId, payload);
        return true;
    }
}
