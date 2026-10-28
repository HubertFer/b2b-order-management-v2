package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderForUpdatePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.RefundPartnerCreditPort;
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

class CancelOrderServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant CANCELLED_AT = CREATED_AT.plusSeconds(60);

    private final List<Order> savedOrders = new ArrayList<>();
    private final List<BigDecimal> refunds = new ArrayList<>();
    private final List<OrderStatusChangedEvent> events = new ArrayList<>();

    @Test
    void cancelsPendingOrderWithoutRefund() {
        Order pending = createOrder();

        Order cancelled = serviceFor(Optional.of(pending)).cancel(pending.id());

        assertEquals(OrderStatus.CANCELADO, cancelled.status());
        assertEquals(CANCELLED_AT, cancelled.updatedAt());
        assertEquals(List.of(), refunds);
        assertEquals(List.of(cancelled), savedOrders);
        assertEquals(OrderStatus.CANCELADO, events.getFirst().newStatus());
    }

    @Test
    void refundsCreditWhenCancellingApprovedOrder() {
        Order approved = createOrder().transitionTo(OrderStatus.APROVADO, CREATED_AT);

        serviceFor(Optional.of(approved)).cancel(approved.id());

        assertEquals(List.of(new BigDecimal("40.00")), refunds);
    }

    @Test
    void refundsCreditWhenCancellingOrderInProcessing() {
        Order processing = createOrder()
                .transitionTo(OrderStatus.APROVADO, CREATED_AT)
                .transitionTo(OrderStatus.EM_PROCESSAMENTO, CREATED_AT);

        serviceFor(Optional.of(processing)).cancel(processing.id());

        assertEquals(List.of(new BigDecimal("40.00")), refunds);
    }

    @Test
    void rejectsCancellingShippedOrderWithoutRefundOrSave() {
        Order shipped = createOrder()
                .transitionTo(OrderStatus.APROVADO, CREATED_AT)
                .transitionTo(OrderStatus.EM_PROCESSAMENTO, CREATED_AT)
                .transitionTo(OrderStatus.ENVIADO, CREATED_AT);

        assertThrows(InvalidOrderTransitionException.class, () -> serviceFor(Optional.of(shipped)).cancel(shipped.id()));

        assertEquals(List.of(), refunds);
        assertEquals(List.of(), savedOrders);
        assertEquals(List.of(), events);
    }

    @Test
    void rejectsMissingOrder() {
        assertThrows(OrderNotFoundException.class, () -> serviceFor(Optional.empty()).cancel(UUID.randomUUID()));
    }

    private CancelOrderService serviceFor(Optional<Order> order) {
        FindOrderForUpdatePort findPort = ignored -> order;
        RefundPartnerCreditPort refundPort = (partnerId, amount) -> refunds.add(amount);
        SaveOrderPort savePort = value -> {
            savedOrders.add(value);
            return value;
        };
        return new CancelOrderService(findPort, refundPort, savePort, events::add, Clock.fixed(CANCELLED_AT, ZoneOffset.UTC));
    }

    private static Order createOrder() {
        return Order.create(UUID.randomUUID(), List.of(new OrderItem(UUID.randomUUID(), 2, new BigDecimal("20.00"))), CREATED_AT);
    }
}
