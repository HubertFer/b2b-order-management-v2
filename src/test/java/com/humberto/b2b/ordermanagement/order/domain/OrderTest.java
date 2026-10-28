package com.humberto.b2b.ordermanagement.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    @Test
    void calculatesTotalFromItems() {
        Order order = Order.create(
                UUID.randomUUID(),
                List.of(
                        new OrderItem(UUID.randomUUID(), 2, new BigDecimal("12.50")),
                        new OrderItem(UUID.randomUUID(), 3, new BigDecimal("0.75"))
                ),
                Instant.parse("2026-10-15T12:00:00Z")
        );

        assertEquals(new BigDecimal("27.25"), order.total());
    }

    @Test
    void createsPendingOrderWithGeneratedIdAndCreationTimestamps() {
        UUID partnerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-15T12:00:00Z");

        Order order = Order.create(partnerId, List.of(new OrderItem(UUID.randomUUID(), 1, BigDecimal.ZERO)), createdAt);

        assertNotNull(order.id());
        assertEquals(partnerId, order.partnerId());
        assertEquals(OrderStatus.PENDENTE, order.status());
        assertEquals(createdAt, order.createdAt());
        assertEquals(createdAt, order.updatedAt());
    }

    @Test
    void copiesItemsAndRejectsInvalidOrderValues() {
        UUID partnerId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        List<OrderItem> items = new ArrayList<>(List.of(new OrderItem(UUID.randomUUID(), 1, BigDecimal.ONE)));
        Order order = Order.create(partnerId, items, createdAt);
        items.clear();

        assertEquals(1, order.items().size());
        assertThrows(UnsupportedOperationException.class, () -> order.items().clear());
        assertThrows(IllegalArgumentException.class, () -> Order.create(partnerId, List.of(), createdAt));
        assertThrows(NullPointerException.class, () -> Order.create(null, items, createdAt));
        assertThrows(NullPointerException.class, () -> Order.create(partnerId, null, createdAt));
    }
}