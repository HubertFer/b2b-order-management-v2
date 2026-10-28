package com.humberto.b2b.ordermanagement.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusChangedEventTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant APPROVED_AT = CREATED_AT.plusSeconds(60);

    @Test
    void describesTransitionBetweenTwoVersionsOfOrder() {
        Order pending = createOrder();
        Order approved = pending.transitionTo(OrderStatus.APROVADO, APPROVED_AT);

        OrderStatusChangedEvent event = OrderStatusChangedEvent.between(pending, approved);

        assertEquals(pending.id(), event.orderId());
        assertEquals(pending.partnerId(), event.partnerId());
        assertEquals(OrderStatus.PENDENTE, event.previousStatus());
        assertEquals(OrderStatus.APROVADO, event.newStatus());
        assertEquals(APPROVED_AT, event.occurredAt());
    }

    @Test
    void generatesUniqueEventIds() {
        Order pending = createOrder();
        Order approved = pending.transitionTo(OrderStatus.APROVADO, APPROVED_AT);

        assertNotEquals(OrderStatusChangedEvent.between(pending, approved).eventId(),
                OrderStatusChangedEvent.between(pending, approved).eventId());
    }

    @Test
    void rejectsDifferentOrders() {
        Order first = createOrder();
        Order second = createOrder().transitionTo(OrderStatus.APROVADO, APPROVED_AT);

        assertThrows(IllegalArgumentException.class, () -> OrderStatusChangedEvent.between(first, second));
    }

    @Test
    void rejectsUnchangedStatus() {
        Order pending = createOrder();

        assertThrows(IllegalArgumentException.class, () -> OrderStatusChangedEvent.between(pending, pending));
    }

    private static Order createOrder() {
        return Order.create(UUID.randomUUID(),
                List.of(new OrderItem(UUID.randomUUID(), 1, new BigDecimal("10.00"))),
                CREATED_AT);
    }
}
