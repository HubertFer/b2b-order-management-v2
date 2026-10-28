package com.humberto.b2b.ordermanagement.notification.infrastructure;

import com.humberto.b2b.ordermanagement.TestcontainersConfiguration;
import com.humberto.b2b.ordermanagement.notification.outbox.OutboxMessage;
import com.humberto.b2b.ordermanagement.notification.outbox.OutboxStore;
import com.humberto.b2b.ordermanagement.notification.outbox.PendingOutboxMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "notification.outbox.relay-interval=PT1H")
class JdbcOutboxStoreConcurrencyTest {

    @Autowired
    private OutboxStore outboxStore;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void concurrentRelaysLockDisjointEvents() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        List<UUID> eventIds = transaction.execute(status -> IntStream.range(0, 4)
                .mapToObj(i -> append())
                .toList());
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<List<UUID>> first = executor.submit(() -> transaction.execute(status -> {
                List<UUID> locked = eventIdsOf(outboxStore.lockPending(2));
                firstLocked.countDown();
                await(releaseFirst);
                return locked;
            }));
            assertTrue(firstLocked.await(10, TimeUnit.SECONDS));

            List<UUID> second = transaction.execute(status -> eventIdsOf(outboxStore.lockPending(10)));
            releaseFirst.countDown();

            assertEquals(eventIds.subList(0, 2), first.get(10, TimeUnit.SECONDS));
            assertEquals(eventIds.subList(2, 4), second);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void lockedEventsBecomeAvailableAgainAfterRollback() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        UUID eventId = transaction.execute(status -> append());

        transaction.executeWithoutResult(status -> {
            assertTrue(eventIdsOf(outboxStore.lockPending(10)).contains(eventId));
            status.setRollbackOnly();
        });

        List<UUID> pending = transaction.execute(status -> {
            List<PendingOutboxMessage> locked = outboxStore.lockPending(10);
            outboxStore.markPublished(locked.stream().map(PendingOutboxMessage::id).toList(), Instant.now());
            return eventIdsOf(locked);
        });
        assertTrue(pending.contains(eventId));
    }

    private UUID append() {
        UUID eventId = UUID.randomUUID();
        outboxStore.append(new OutboxMessage(eventId, "ORDER", UUID.randomUUID(), "ORDER_STATUS_CHANGED",
                "{}", Instant.now()));
        return eventId;
    }

    private static List<UUID> eventIdsOf(List<PendingOutboxMessage> pending) {
        return pending.stream().map(message -> message.message().eventId()).toList();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the second relay");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
