package com.humberto.b2b.ordermanagement.notification.outbox;

import java.time.Instant;
import java.util.List;

public interface OutboxStore {

    void append(OutboxMessage message);

    List<PendingOutboxMessage> lockPending(int limit);

    void markPublished(List<Long> ids, Instant publishedAt);
}
