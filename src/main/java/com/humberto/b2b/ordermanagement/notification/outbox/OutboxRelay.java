package com.humberto.b2b.ordermanagement.notification.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxStore outboxStore;
    private final NotificationPublisher notificationPublisher;
    private final TransactionOperations transactionOperations;
    private final Clock clock;
    private final int batchSize;

    public OutboxRelay(OutboxStore outboxStore,
                       NotificationPublisher notificationPublisher,
                       TransactionOperations transactionOperations,
                       Clock clock,
                       int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be greater than zero");
        }
        this.outboxStore = outboxStore;
        this.notificationPublisher = notificationPublisher;
        this.transactionOperations = transactionOperations;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    public int relayPending() {
        Integer published = transactionOperations.execute(status -> publishBatch());
        return published == null ? 0 : published;
    }

    public int drain(Duration maxDuration) {
        Instant deadline = clock.instant().plus(maxDuration);
        int total = 0;
        int published;
        do {
            published = relayPending();
            total += published;
        } while (published == batchSize && clock.instant().isBefore(deadline));
        return total;
    }

    private int publishBatch() {
        List<Long> publishedIds = new ArrayList<>();
        for (PendingOutboxMessage pending : outboxStore.lockPending(batchSize)) {
            try {
                notificationPublisher.publish(pending.message());
            } catch (RuntimeException exception) {
                log.warn("Failed to publish outbox event {}, will retry", pending.message().eventId(), exception);
                break;
            }
            publishedIds.add(pending.id());
        }
        outboxStore.markPublished(publishedIds, clock.instant());
        return publishedIds.size();
    }
}
