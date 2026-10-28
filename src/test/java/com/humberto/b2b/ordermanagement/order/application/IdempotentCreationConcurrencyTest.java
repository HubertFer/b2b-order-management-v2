package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.TestcontainersConfiguration;
import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class IdempotentCreationConcurrencyTest {

    @Autowired
    private CreateOrderService createOrderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentCreationsWithSameIdempotencyKeyCreateOneOrder() throws Exception {
        UUID partnerId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO partner (id, credit_limit, used_credit) VALUES (?, 1000.00, 0)", partnerId);
        List<OrderItem> items = List.of(new OrderItem(UUID.randomUUID(), 1, new BigDecimal("10.00")));
        int requests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<OrderCreation>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return createOrderService.create(partnerId, items, "retry-key");
                }));
            }
            start.countDown();

            List<OrderCreation> creations = new ArrayList<>();
            for (Future<OrderCreation> future : futures) {
                creations.add(future.get(30, TimeUnit.SECONDS));
            }

            assertEquals(1, creations.stream().filter(creation -> !creation.replayed()).count());
            assertEquals(1, creations.stream().map(creation -> creation.order().id()).distinct().count());
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM orders WHERE partner_id = ?", Integer.class, partnerId));
        } finally {
            executor.shutdownNow();
        }
    }
}
