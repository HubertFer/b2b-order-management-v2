package com.humberto.b2b.ordermanagement.order.infrastructure.notification;

import com.humberto.b2b.ordermanagement.TestcontainersConfiguration;
import com.humberto.b2b.ordermanagement.notification.outbox.OutboxMessage;
import com.humberto.b2b.ordermanagement.notification.outbox.OutboxRelay;
import com.humberto.b2b.ordermanagement.notification.outbox.OutboxStore;
import com.humberto.b2b.ordermanagement.order.application.ApproveOrderService;
import com.humberto.b2b.ordermanagement.order.application.CancelOrderService;
import com.humberto.b2b.ordermanagement.order.application.CreateOrderService;
import com.humberto.b2b.ordermanagement.order.application.InsufficientOrderCreditException;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderStatusNotificationTest {

    @Autowired
    private CreateOrderService createOrderService;

    @Autowired
    private ApproveOrderService approveOrderService;

    @Autowired
    private CancelOrderService cancelOrderService;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private OutboxStore outboxStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void recordsEachStatusChangeInOutbox() {
        UUID partnerId = insertPartner("100.00");
        Order order = createOrder(partnerId, "30.00");

        approveOrderService.approve(order.id());
        cancelOrderService.cancel(order.id());

        List<Map<String, Object>> events = outboxEvents(order.id());
        assertEquals(2, events.size());
        assertEquals("ORDER", events.get(0).get("aggregate_type"));
        assertEquals("ORDER_STATUS_CHANGED", events.get(0).get("event_type"));
        assertEquals("PENDENTE", events.get(0).get("previous_status"));
        assertEquals("APROVADO", events.get(0).get("new_status"));
        assertEquals(partnerId.toString(), events.get(0).get("partner_id"));
        assertEquals("APROVADO", events.get(1).get("previous_status"));
        assertEquals("CANCELADO", events.get(1).get("new_status"));
    }

    @Test
    void doesNotRecordEventWhenStatusChangeFails() {
        UUID partnerId = insertPartner("100.00");
        Order first = createOrder(partnerId, "60.00");
        Order second = createOrder(partnerId, "60.00");
        approveOrderService.approve(first.id());

        assertThrows(InsufficientOrderCreditException.class, () -> approveOrderService.approve(second.id()));

        assertEquals(1, outboxEvents(first.id()).size());
        assertEquals(List.of(), outboxEvents(second.id()));
    }

    @Test
    void relayMarksRecordedEventsAsPublished() {
        Order order = createOrder(insertPartner("100.00"), "10.00");
        approveOrderService.approve(order.id());

        for (int attempt = 0; attempt < 100 && publishedAt(order.id()) == null; attempt++) {
            outboxRelay.relayPending();
        }

        assertNotNull(publishedAt(order.id()));
    }

    @Test
    void requiresTransactionToAppendToOutbox() {
        OutboxMessage message = new OutboxMessage(UUID.randomUUID(), "ORDER", UUID.randomUUID(),
                "ORDER_STATUS_CHANGED", "{}", Instant.now());

        assertThrows(IllegalTransactionStateException.class, () -> outboxStore.append(message));
        assertTrue(outboxEvents(message.aggregateId()).isEmpty());
    }

    private Order createOrder(UUID partnerId, String unitPrice) {
        return createOrderService.create(partnerId,
                List.of(new OrderItem(UUID.randomUUID(), 1, new BigDecimal(unitPrice))));
    }

    private List<Map<String, Object>> outboxEvents(UUID orderId) {
        return jdbcTemplate.queryForList("""
                SELECT aggregate_type, event_type,
                       payload ->> 'previousStatus' AS previous_status,
                       payload ->> 'newStatus' AS new_status,
                       payload ->> 'partnerId' AS partner_id
                  FROM outbox_event
                 WHERE aggregate_id = ?
                 ORDER BY id
                """, orderId);
    }

    private Object publishedAt(UUID orderId) {
        return jdbcTemplate.queryForObject("SELECT max(published_at) FROM outbox_event WHERE aggregate_id = ?",
                Object.class, orderId);
    }

    private UUID insertPartner(String creditLimit) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO partner (id, credit_limit, used_credit) VALUES (?, ?, 0)",
                id, new BigDecimal(creditLimit));
        return id;
    }
}
