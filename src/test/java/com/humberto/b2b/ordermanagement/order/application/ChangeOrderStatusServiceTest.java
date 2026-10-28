package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderForUpdatePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SaveOrderPort;
import com.humberto.b2b.ordermanagement.order.domain.InvalidOrderTransitionException;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatusChangedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChangeOrderStatusServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant CHANGED_AT = CREATED_AT.plusSeconds(60);

    private final List<Order> savedOrders = new ArrayList<>();
    private final List<OrderStatusChangedEvent> events = new ArrayList<>();

    @Test
    void movesApprovedOrderToProcessing() {
        Order approved = createOrder().transitionTo(OrderStatus.APROVADO, CREATED_AT);

        Order processing = serviceFor(approved).changeStatus(approved.id(), OrderStatus.EM_PROCESSAMENTO);

        assertEquals(OrderStatus.EM_PROCESSAMENTO, processing.status());
        assertEquals(CHANGED_AT, processing.updatedAt());
        assertEquals(List.of(processing), savedOrders);
        assertEquals(OrderStatus.APROVADO, events.getFirst().previousStatus());
        assertEquals(OrderStatus.EM_PROCESSAMENTO, events.getFirst().newStatus());
    }

    @Test
    void refusesStatusesThatAffectCreditBeforeLoadingOrder() {
        ChangeOrderStatusService service = new ChangeOrderStatusService(
                id -> {
                    throw new AssertionError("order must not be loaded");
                },
                value -> value,
                events::add,
                Clock.fixed(CHANGED_AT, ZoneOffset.UTC));

        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(UUID.randomUUID(), OrderStatus.APROVADO));
        assertThrows(IllegalArgumentException.class, () -> service.changeStatus(UUID.randomUUID(), OrderStatus.CANCELADO));
    }

    @Test
    void rejectsTransitionNotAllowedByLifecycle() {
        Order pending = createOrder();

        assertThrows(InvalidOrderTransitionException.class,
                () -> serviceFor(pending).changeStatus(pending.id(), OrderStatus.ENVIADO));
        assertEquals(List.of(), savedOrders);
    }

    private ChangeOrderStatusService serviceFor(Order order) {
        FindOrderForUpdatePort findPort = ignored -> Optional.of(order);
        SaveOrderPort savePort = value -> {
            savedOrders.add(value);
            return value;
        };
        return new ChangeOrderStatusService(findPort, savePort, events::add, Clock.fixed(CHANGED_AT, ZoneOffset.UTC));
    }

    private static Order createOrder() {
        return Order.create(UUID.randomUUID(), List.of(new OrderItem(UUID.randomUUID(), 1, BigDecimal.TEN)), CREATED_AT);
    }
}
