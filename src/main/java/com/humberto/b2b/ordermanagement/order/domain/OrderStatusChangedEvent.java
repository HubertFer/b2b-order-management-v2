package com.humberto.b2b.ordermanagement.order.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID eventId,
        UUID orderId,
        UUID partnerId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant occurredAt
) {

    public static OrderStatusChangedEvent between(Order previous, Order current) {
        Objects.requireNonNull(previous, "previous must not be null");
        Objects.requireNonNull(current, "current must not be null");
        if (!previous.id().equals(current.id())) {
            throw new IllegalArgumentException("previous and current must be the same order");
        }
        if (previous.status() == current.status()) {
            throw new IllegalArgumentException("status did not change");
        }
        return new OrderStatusChangedEvent(UUID.randomUUID(), current.id(), current.partnerId(),
                previous.status(), current.status(), current.updatedAt());
    }
}
