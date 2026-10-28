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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertNotSame;

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

    @ParameterizedTest
    @CsvSource({
            "PENDENTE, APROVADO",
            "PENDENTE, CANCELADO",
            "APROVADO, EM_PROCESSAMENTO",
            "APROVADO, CANCELADO",
            "EM_PROCESSAMENTO, ENVIADO",
            "EM_PROCESSAMENTO, CANCELADO",
            "ENVIADO, ENTREGUE"
    })
    void allowsValidTransitionsWithoutChangingOriginal(OrderStatus from, OrderStatus to) {
        Instant createdAt = Instant.parse("2026-10-15T12:00:00Z");
        Instant changedAt = createdAt.plusSeconds(60);
        Order original = orderWithStatus(from, createdAt);

        Order updated = original.transitionTo(to, changedAt);

        assertNotSame(original, updated);
        assertEquals(from, original.status());
        assertEquals(createdAt, original.updatedAt());
        assertEquals(to, updated.status());
        assertEquals(createdAt, updated.createdAt());
        assertEquals(changedAt, updated.updatedAt());
    }

    @ParameterizedTest
    @CsvSource({
            "PENDENTE, PENDENTE",
            "PENDENTE, EM_PROCESSAMENTO",
            "APROVADO, ENVIADO",
            "ENVIADO, CANCELADO",
            "ENTREGUE, CANCELADO",
            "CANCELADO, PENDENTE"
    })
    void rejectsInvalidTransitions(OrderStatus from, OrderStatus to) {
        Instant timestamp = Instant.parse("2026-10-15T12:00:00Z");
        Order order = orderWithStatus(from, timestamp);

        assertThrows(InvalidOrderTransitionException.class, () -> order.transitionTo(to, timestamp));
    }

    @Test
    void rejectsNullTransitionArguments() {
        Order order = orderWithStatus(OrderStatus.PENDENTE, Instant.parse("2026-10-15T12:00:00Z"));

        assertThrows(NullPointerException.class, () -> order.transitionTo(null, Instant.now()));
        assertThrows(NullPointerException.class, () -> order.transitionTo(OrderStatus.APROVADO, null));
    }

    @Test
    void restoresOrderWithAllStoredValues() {
        UUID id = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-15T12:00:00Z");
        Instant updatedAt = createdAt.plusSeconds(120);
        List<OrderItem> items = new ArrayList<>(List.of(new OrderItem(UUID.randomUUID(), 2, new BigDecimal("10.00"))));

        Order order = Order.restore(id, partnerId, items, new BigDecimal("20.00"),
                OrderStatus.EM_PROCESSAMENTO, createdAt, updatedAt);
        items.clear();

        assertEquals(id, order.id());
        assertEquals(partnerId, order.partnerId());
        assertEquals(1, order.items().size());
        assertEquals(new BigDecimal("20.00"), order.total());
        assertEquals(OrderStatus.EM_PROCESSAMENTO, order.status());
        assertEquals(createdAt, order.createdAt());
        assertEquals(updatedAt, order.updatedAt());
        assertThrows(UnsupportedOperationException.class, () -> order.items().clear());
    }

    @Test
    void rejectsInvalidRestoredValues() {
        UUID id = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        List<OrderItem> items = List.of(new OrderItem(UUID.randomUUID(), 1, BigDecimal.ONE));
        Instant now = Instant.parse("2026-10-15T12:00:00Z");

        assertThrows(NullPointerException.class,
                () -> Order.restore(null, partnerId, items, BigDecimal.ONE, OrderStatus.PENDENTE, now, now));
        assertThrows(NullPointerException.class,
                () -> Order.restore(id, partnerId, items, null, OrderStatus.PENDENTE, now, now));
        assertThrows(NullPointerException.class,
                () -> Order.restore(id, partnerId, items, BigDecimal.ONE, null, now, now));
        assertThrows(IllegalArgumentException.class,
                () -> Order.restore(id, partnerId, List.of(), BigDecimal.ONE, OrderStatus.PENDENTE, now, now));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDENTE, false",
            "APROVADO, true",
            "EM_PROCESSAMENTO, true",
            "ENVIADO, true",
            "ENTREGUE, true",
            "CANCELADO, false"
    })
    void knowsWhetherItsTotalIsDebitedFromPartnerCredit(OrderStatus status, boolean debited) {
        assertEquals(debited, orderWithStatus(status, Instant.parse("2026-10-15T12:00:00Z")).hasDebitedCredit());
    }

    private static Order orderWithStatus(OrderStatus status, Instant createdAt) {
        Order order = Order.create(
                UUID.randomUUID(),
                List.of(new OrderItem(UUID.randomUUID(), 1, BigDecimal.ONE)),
                createdAt
        );
        return switch (status) {
            case PENDENTE -> order;
            case APROVADO -> order.transitionTo(OrderStatus.APROVADO, createdAt);
            case EM_PROCESSAMENTO -> order.transitionTo(OrderStatus.APROVADO, createdAt)
                    .transitionTo(OrderStatus.EM_PROCESSAMENTO, createdAt);
            case ENVIADO -> order.transitionTo(OrderStatus.APROVADO, createdAt)
                    .transitionTo(OrderStatus.EM_PROCESSAMENTO, createdAt)
                    .transitionTo(OrderStatus.ENVIADO, createdAt);
            case ENTREGUE -> order.transitionTo(OrderStatus.APROVADO, createdAt)
                    .transitionTo(OrderStatus.EM_PROCESSAMENTO, createdAt)
                    .transitionTo(OrderStatus.ENVIADO, createdAt)
                    .transitionTo(OrderStatus.ENTREGUE, createdAt);
            case CANCELADO -> order.transitionTo(OrderStatus.CANCELADO, createdAt);
        };
    }
}