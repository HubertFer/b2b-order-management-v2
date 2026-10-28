package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.DebitPartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderForUpdatePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.PartnerCreditPort;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApproveOrderServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant APPROVED_AT = CREATED_AT.plusSeconds(60);

    private final List<Order> savedOrders = new ArrayList<>();
    private final AtomicBoolean debitAttempted = new AtomicBoolean();
    private final List<OrderStatusChangedEvent> events = new ArrayList<>();

    @Test
    void approvesOrderAfterSuccessfulCreditDebit() {
        UUID partnerId = UUID.randomUUID();
        Order order = createOrder(partnerId);
        ApproveOrderService service = serviceFor(Optional.of(order), (id, amount) -> {
            assertEquals(partnerId, id);
            assertEquals(new BigDecimal("25.00"), amount);
            debitAttempted.set(true);
            return true;
        });

        Order approved = service.approve(order.id());

        assertTrue(debitAttempted.get());
        assertEquals(OrderStatus.APROVADO, approved.status());
        assertEquals(APPROVED_AT, approved.updatedAt());
        assertEquals(OrderStatus.PENDENTE, order.status());
        assertEquals(List.of(approved), savedOrders);
        assertEquals(1, events.size());
        assertEquals(OrderStatus.PENDENTE, events.getFirst().previousStatus());
        assertEquals(OrderStatus.APROVADO, events.getFirst().newStatus());
    }

    @Test
    void rejectsMissingOrderWithoutDebitOrSave() {
        ApproveOrderService service = serviceFor(Optional.empty(), succeedingDebit());

        assertThrows(OrderNotFoundException.class, () -> service.approve(UUID.randomUUID()));

        assertEquals(List.of(), savedOrders);
        assertFalse(debitAttempted.get());
    }

    @Test
    void rejectsInvalidStatusBeforeAttemptingDebit() {
        Order alreadyApproved = createOrder(UUID.randomUUID()).transitionTo(OrderStatus.APROVADO, CREATED_AT);
        ApproveOrderService service = serviceFor(Optional.of(alreadyApproved), succeedingDebit());

        assertThrows(InvalidOrderTransitionException.class, () -> service.approve(alreadyApproved.id()));

        assertEquals(List.of(), savedOrders);
        assertFalse(debitAttempted.get());
    }

    @Test
    void rejectsInsufficientCreditReportingAvailableCreditWithoutSaving() {
        Order order = createOrder(UUID.randomUUID());
        ApproveOrderService service = serviceFor(Optional.of(order), (id, amount) -> false);

        InsufficientOrderCreditException exception = assertThrows(InsufficientOrderCreditException.class,
                () -> service.approve(order.id()));

        assertEquals(new BigDecimal("10.00"), exception.availableCredit());
        assertEquals(new BigDecimal("25.00"), exception.requestedAmount());
        assertEquals(List.of(), savedOrders);
        assertEquals(List.of(), events);
    }

    private DebitPartnerCreditPort succeedingDebit() {
        return (id, amount) -> {
            debitAttempted.set(true);
            return true;
        };
    }

    private ApproveOrderService serviceFor(Optional<Order> order, DebitPartnerCreditPort debitPort) {
        FindOrderForUpdatePort findPort = ignored -> order;
        PartnerCreditPort partnerCreditPort = ignored -> Optional.of(new BigDecimal("10.00"));
        SaveOrderPort savePort = value -> {
            savedOrders.add(value);
            return value;
        };
        return new ApproveOrderService(findPort, debitPort, partnerCreditPort, savePort, events::add,
                Clock.fixed(APPROVED_AT, ZoneOffset.UTC));
    }

    private static Order createOrder(UUID partnerId) {
        return Order.create(partnerId, List.of(new OrderItem(UUID.randomUUID(), 1, new BigDecimal("25.00"))), CREATED_AT);
    }
}
