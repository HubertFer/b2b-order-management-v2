package com.humberto.b2b.ordermanagement.notification.inbox;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedEventStore {

    boolean markProcessed(UUID eventId, Instant processedAt);
}
