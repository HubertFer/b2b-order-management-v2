package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.PartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SaveOrderPort;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
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

class CreateOrderServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-15T12:00:00Z");

    private final List<Order> savedOrders = new ArrayList<>();

    @Test
    void createsAndSavesPendingOrder() {
        CreateOrderService service = serviceWithAvailableCredit(Optional.of(new BigDecimal("100.00")));

        Order order = service.create(UUID.randomUUID(), List.of(item(2, "15.00")));

        assertEquals(OrderStatus.PENDENTE, order.status());
        assertEquals(new BigDecimal("30.00"), order.total());
        assertEquals(CREATED_AT, order.createdAt());
        assertEquals(List.of(order), savedOrders);
    }

    @Test
    void acceptsOrderThatUsesExactlyTheAvailableCredit() {
        CreateOrderService service = serviceWithAvailableCredit(Optional.of(new BigDecimal("20.00")));

        service.create(UUID.randomUUID(), List.of(item(2, "10.00")));

        assertEquals(1, savedOrders.size());
    }

    @Test
    void doesNotSaveWhenPartnerDoesNotExist() {
        CreateOrderService service = serviceWithAvailableCredit(Optional.empty());

        assertThrows(PartnerNotFoundException.class,
                () -> service.create(UUID.randomUUID(), List.of(item(1, "10.00"))));
        assertEquals(List.of(), savedOrders);
    }

    @Test
    void doesNotSaveWhenCreditIsInsufficientAndReportsAvailableCredit() {
        CreateOrderService service = serviceWithAvailableCredit(Optional.of(new BigDecimal("10.00")));

        InsufficientOrderCreditException exception = assertThrows(InsufficientOrderCreditException.class,
                () -> service.create(UUID.randomUUID(), List.of(item(2, "10.00"))));

        assertEquals(new BigDecimal("10.00"), exception.availableCredit());
        assertEquals(new BigDecimal("20.00"), exception.requestedAmount());
        assertEquals(List.of(), savedOrders);
    }

    @Test
    void doesNotSaveAnInvalidOrder() {
        CreateOrderService service = serviceWithAvailableCredit(Optional.of(new BigDecimal("100.00")));

        assertThrows(IllegalArgumentException.class, () -> service.create(UUID.randomUUID(), List.of()));
        assertEquals(List.of(), savedOrders);
    }

    private CreateOrderService serviceWithAvailableCredit(Optional<BigDecimal> availableCredit) {
        PartnerCreditPort partnerCreditPort = partnerId -> availableCredit;
        SaveOrderPort saveOrderPort = order -> {
            savedOrders.add(order);
            return order;
        };
        return new CreateOrderService(partnerCreditPort, saveOrderPort, Clock.fixed(CREATED_AT, ZoneOffset.UTC));
    }

    private static OrderItem item(int quantity, String unitPrice) {
        return new OrderItem(UUID.randomUUID(), quantity, new BigDecimal(unitPrice));
    }
}
