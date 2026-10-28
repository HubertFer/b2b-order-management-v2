package com.humberto.b2b.ordermanagement.notification.outbox;

public record PendingOutboxMessage(long id, OutboxMessage message) {
}
