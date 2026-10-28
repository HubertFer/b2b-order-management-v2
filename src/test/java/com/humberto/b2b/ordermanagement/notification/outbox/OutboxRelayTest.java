package com.humberto.b2b.ordermanagement.notification.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-10-16T12:00:00Z");

    private final InMemoryOutboxStore store = new InMemoryOutboxStore();
    private final List<OutboxMessage> published = new ArrayList<>();

    @Test
    void publishesPendingMessagesInOrderAndMarksThemPublished() {
        OutboxMessage first = store.add();
        OutboxMessage second = store.add();

        int count = relay(published::add, 10).relayPending();

        assertEquals(2, count);
        assertEquals(List.of(first, second), published);
        assertEquals(List.of(), store.lockPending(10));
        assertEquals(List.of(NOW, NOW), List.copyOf(store.publishedAt.values()));
    }

    @Test
    void respectsBatchSize() {
        OutboxMessage first = store.add();
        OutboxMessage second = store.add();

        relay(published::add, 1).relayPending();

        assertEquals(List.of(first), published);
        assertEquals(second, store.lockPending(10).getFirst().message());
    }

    @Test
    void stopsAtFirstFailureAndKeepsRemainingMessagesPending() {
        OutboxMessage first = store.add();
        OutboxMessage failing = store.add();
        OutboxMessage third = store.add();
        NotificationPublisher publisher = message -> {
            if (message.equals(failing)) {
                throw new IllegalStateException("broker unavailable");
            }
            published.add(message);
        };

        int count = relay(publisher, 10).relayPending();

        assertEquals(1, count);
        assertEquals(List.of(first), published);
        assertEquals(List.of(failing, third),
                store.lockPending(10).stream().map(PendingOutboxMessage::message).toList());
    }

    @Test
    void rejectsInvalidBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> relay(published::add, 0));
    }

    @Test
    void drainPublishesFullBatchesUntilBacklogIsEmpty() {
        for (int i = 0; i < 7; i++) {
            store.add();
        }

        int count = relay(published::add, 3).drain(Duration.ofSeconds(10));

        assertEquals(7, count);
        assertEquals(7, published.size());
        assertEquals(List.of(), store.lockPending(10));
    }

    @Test
    void drainStopsAtFirstFailure() {
        OutboxMessage first = store.add();
        OutboxMessage failing = store.add();
        store.add();
        store.add();
        NotificationPublisher publisher = message -> {
            if (message.equals(failing)) {
                throw new IllegalStateException("broker unavailable");
            }
            published.add(message);
        };

        int count = relay(publisher, 1).drain(Duration.ofSeconds(10));

        assertEquals(1, count);
        assertEquals(List.of(first), published);
        assertEquals(3, store.lockPending(10).size());
    }

    @Test
    void drainStopsWhenTimeIsUp() {
        for (int i = 0; i < 10; i++) {
            store.add();
        }
        Clock oneSecondPerRead = new Clock() {
            private Instant current = NOW;

            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                Instant value = current;
                current = current.plusSeconds(1);
                return value;
            }
        };
        OutboxRelay relay = new OutboxRelay(store, published::add, TransactionOperations.withoutTransaction(),
                oneSecondPerRead, 2);

        relay.drain(Duration.ofSeconds(5));

        assertEquals(6, published.size());
        assertEquals(4, store.lockPending(10).size());
    }

    private OutboxRelay relay(NotificationPublisher publisher, int batchSize) {
        return new OutboxRelay(store, publisher, TransactionOperations.withoutTransaction(),
                Clock.fixed(NOW, ZoneOffset.UTC), batchSize);
    }

    private static class InMemoryOutboxStore implements OutboxStore {

        private final List<PendingOutboxMessage> messages = new ArrayList<>();
        private final Map<Long, Instant> publishedAt = new LinkedHashMap<>();

        OutboxMessage add() {
            OutboxMessage message = new OutboxMessage(UUID.randomUUID(), "ORDER", UUID.randomUUID(),
                    "ORDER_STATUS_CHANGED", "{}", NOW);
            append(message);
            return message;
        }

        @Override
        public void append(OutboxMessage message) {
            messages.add(new PendingOutboxMessage(messages.size() + 1, message));
        }

        @Override
        public List<PendingOutboxMessage> lockPending(int limit) {
            return messages.stream()
                    .filter(pending -> !publishedAt.containsKey(pending.id()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public void markPublished(List<Long> ids, Instant publishedAt) {
            ids.forEach(id -> this.publishedAt.put(id, publishedAt));
        }
    }
}
