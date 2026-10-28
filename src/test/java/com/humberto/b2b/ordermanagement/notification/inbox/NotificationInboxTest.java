package com.humberto.b2b.ordermanagement.notification.inbox;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationInboxTest {

    private final Set<UUID> processed = new HashSet<>();
    private final List<UUID> notified = new ArrayList<>();
    private final NotificationInbox inbox = new NotificationInbox(
            (eventId, processedAt) -> processed.add(eventId),
            (orderId, payload) -> notified.add(orderId),
            Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void notifiesOnFirstDeliveryAndIgnoresDuplicates() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        assertTrue(inbox.receive(eventId, orderId, "{}"));
        assertFalse(inbox.receive(eventId, orderId, "{}"));

        assertEquals(List.of(orderId), notified);
    }

    @Test
    void notifiesDifferentEventsOfSameOrder() {
        UUID orderId = UUID.randomUUID();

        inbox.receive(UUID.randomUUID(), orderId, "{}");
        inbox.receive(UUID.randomUUID(), orderId, "{}");

        assertEquals(List.of(orderId, orderId), notified);
    }
}
