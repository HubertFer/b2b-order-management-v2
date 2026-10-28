package com.humberto.b2b.ordermanagement.order.infrastructure.persistence;

import com.humberto.b2b.ordermanagement.TestcontainersConfiguration;
import com.humberto.b2b.ordermanagement.order.application.OrderPage;
import com.humberto.b2b.ordermanagement.order.application.OrderSearchCriteria;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class OrderSearchAdapterTest {

    private static final Instant BASE = Instant.parse("2031-01-01T00:00:00Z");

    @Autowired
    private OrderPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Order oldPending;
    private Order middleApproved;
    private Order newPending;

    @BeforeEach
    void setUp() {
        UUID partnerId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO partner (id, credit_limit, used_credit) VALUES (?, 1000.00, 0)", partnerId);
        oldPending = adapter.save(order(partnerId, BASE));
        middleApproved = adapter.save(order(partnerId, BASE.plusSeconds(60)).transitionTo(OrderStatus.APROVADO, BASE.plusSeconds(61)));
        newPending = adapter.save(order(partnerId, BASE.plusSeconds(120)));
    }

    @Test
    void returnsOrdersInPeriodNewestFirst() {
        OrderPage page = adapter.search(new OrderSearchCriteria(null, BASE, BASE.plusSeconds(180)), 0, 10);

        assertEquals(List.of(newPending.id(), middleApproved.id(), oldPending.id()), ids(page));
        assertEquals(3, page.totalElements());
    }

    @Test
    void filtersByStatusAndPeriod() {
        OrderPage page = adapter.search(new OrderSearchCriteria(OrderStatus.PENDENTE, BASE, BASE.plusSeconds(180)), 0, 10);

        assertEquals(List.of(newPending.id(), oldPending.id()), ids(page));
    }

    @Test
    void periodIncludesStartAndExcludesEnd() {
        OrderPage page = adapter.search(new OrderSearchCriteria(null, BASE.plusSeconds(60), BASE.plusSeconds(120)), 0, 10);

        assertEquals(List.of(middleApproved.id()), ids(page));
    }

    @Test
    void paginatesResults() {
        OrderSearchCriteria criteria = new OrderSearchCriteria(null, BASE, BASE.plusSeconds(180));

        OrderPage first = adapter.search(criteria, 0, 2);
        OrderPage second = adapter.search(criteria, 1, 2);

        assertEquals(List.of(newPending.id(), middleApproved.id()), ids(first));
        assertEquals(List.of(oldPending.id()), ids(second));
        assertEquals(3, first.totalElements());
        assertEquals(2, first.totalPages());
    }

    private static Order order(UUID partnerId, Instant createdAt) {
        return Order.create(partnerId, List.of(new OrderItem(UUID.randomUUID(), 1, new BigDecimal("10.00"))), createdAt);
    }

    private static List<UUID> ids(OrderPage page) {
        return page.orders().stream().map(Order::id).toList();
    }
}
