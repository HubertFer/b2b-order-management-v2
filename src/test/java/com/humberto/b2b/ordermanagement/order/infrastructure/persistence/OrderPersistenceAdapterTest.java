package com.humberto.b2b.ordermanagement.order.infrastructure.persistence;

import com.humberto.b2b.ordermanagement.TestcontainersConfiguration;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class OrderPersistenceAdapterTest {

    private static final Instant CREATED_AT = Instant.parse("2026-10-15T12:00:00Z");

    @Autowired
    private OrderPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void savesAndLoadsOrderWithItems() {
        OrderItem first = new OrderItem(UUID.randomUUID(), 2, new BigDecimal("10.00"));
        OrderItem second = new OrderItem(UUID.randomUUID(), 1, new BigDecimal("5.50"));
        Order order = Order.create(insertPartner(), List.of(first, second), CREATED_AT);

        adapter.save(order);
        clearPersistenceContext();
        Order loaded = adapter.findByIdForUpdate(order.id()).orElseThrow();

        assertEquals(order.id(), loaded.id());
        assertEquals(order.partnerId(), loaded.partnerId());
        assertEquals(new BigDecimal("25.50"), loaded.total());
        assertEquals(OrderStatus.PENDENTE, loaded.status());
        assertEquals(CREATED_AT, loaded.createdAt());
        assertEquals(CREATED_AT, loaded.updatedAt());
        assertEquals(2, loaded.items().size());
        assertTrue(loaded.items().containsAll(List.of(first, second)));
    }

    @Test
    void updatesStatusAndTimestampOfExistingOrder() {
        Order order = Order.create(insertPartner(),
                List.of(new OrderItem(UUID.randomUUID(), 1, new BigDecimal("30.00"))), CREATED_AT);
        adapter.save(order);
        clearPersistenceContext();
        Instant approvedAt = CREATED_AT.plusSeconds(60);

        adapter.save(order.transitionTo(OrderStatus.APROVADO, approvedAt));
        clearPersistenceContext();
        Order loaded = adapter.findByIdForUpdate(order.id()).orElseThrow();

        assertEquals(OrderStatus.APROVADO, loaded.status());
        assertEquals(CREATED_AT, loaded.createdAt());
        assertEquals(approvedAt, loaded.updatedAt());
        assertEquals(order.items(), loaded.items());
        assertEquals(1, countItems(order.id()));
    }

    @Test
    void returnsEmptyForUnknownOrder() {
        assertTrue(adapter.findByIdForUpdate(UUID.randomUUID()).isEmpty());
    }

    private UUID insertPartner() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO partner (id, credit_limit, used_credit) VALUES (?, 1000.00, 0)", id);
        return id;
    }

    private int countItems(UUID orderId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM order_item WHERE order_id = ?",
                Integer.class, orderId);
    }

    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }
}
