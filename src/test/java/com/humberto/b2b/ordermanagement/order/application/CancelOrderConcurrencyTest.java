package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.TestcontainersConfiguration;
import com.humberto.b2b.ordermanagement.order.domain.InvalidOrderTransitionException;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CancelOrderConcurrencyTest {

    @Autowired
    private CreateOrderService createOrderService;

    @Autowired
    private ApproveOrderService approveOrderService;

    @Autowired
    private CancelOrderService cancelOrderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void concurrentCancellationsOfApprovedOrderRefundOnlyOnce() throws Exception {
        UUID partnerId = insertPartner();
        UUID orderId = createOrder(partnerId);
        approveOrderService.approve(orderId);

        List<Throwable> outcomes = runConcurrently(List.of(
                () -> cancelOrderService.cancel(orderId),
                () -> cancelOrderService.cancel(orderId),
                () -> cancelOrderService.cancel(orderId),
                () -> cancelOrderService.cancel(orderId),
                () -> cancelOrderService.cancel(orderId)));

        assertEquals(1, outcomes.stream().filter(outcome -> outcome == null).count());
        assertEquals(4, outcomes.stream().filter(InvalidOrderTransitionException.class::isInstance).count());
        assertEquals(new BigDecimal("0.00"), usedCredit(partnerId));
        assertEquals("CANCELADO", status(orderId));
    }

    @Test
    void approvalRacingCancellationNeverLeavesCreditDebitedForCancelledOrder() throws Exception {
        for (int round = 0; round < 10; round++) {
            UUID partnerId = insertPartner();
            UUID orderId = createOrder(partnerId);

            List<Throwable> outcomes = runConcurrently(List.of(
                    () -> approveOrderService.approve(orderId),
                    () -> cancelOrderService.cancel(orderId)));

            assertTrue(outcomes.stream().allMatch(outcome -> outcome == null || outcome instanceof InvalidOrderTransitionException));
            assertEquals("CANCELADO", status(orderId));
            assertEquals(new BigDecimal("0.00"), usedCredit(partnerId));
        }
    }

    private List<Throwable> runConcurrently(List<Callable<?>> tasks) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Callable<?> task : tasks) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();

            List<Throwable> outcomes = new ArrayList<>();
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                    outcomes.add(null);
                } catch (ExecutionException e) {
                    outcomes.add(e.getCause());
                } catch (Exception e) {
                    outcomes.add(e);
                }
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    private UUID createOrder(UUID partnerId) {
        return createOrderService.create(partnerId, List.of(new OrderItem(UUID.randomUUID(), 1, new BigDecimal("30.00")))).id();
    }

    private UUID insertPartner() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO partner (id, credit_limit, used_credit) VALUES (?, 100.00, 0)", id);
        return id;
    }

    private BigDecimal usedCredit(UUID partnerId) {
        return jdbcTemplate.queryForObject("SELECT used_credit FROM partner WHERE id = ?", BigDecimal.class, partnerId);
    }

    private String status(UUID orderId) {
        return jdbcTemplate.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }
}
