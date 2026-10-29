package com.humberto.b2b.ordermanagement.notification.infrastructure.kafka;

import com.humberto.b2b.ordermanagement.KafkaTestcontainersConfiguration;
import com.humberto.b2b.ordermanagement.TestcontainersConfiguration;
import com.humberto.b2b.ordermanagement.notification.inbox.PartnerNotifier;
import com.humberto.b2b.ordermanagement.order.application.ApproveOrderService;
import com.humberto.b2b.ordermanagement.order.application.CreateOrderService;
import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import({TestcontainersConfiguration.class, KafkaTestcontainersConfiguration.class})
@SpringBootTest(properties = "notification.publisher=kafka")
class KafkaNotificationTest {

    @Autowired
    private CreateOrderService createOrderService;

    @Autowired
    private ApproveOrderService approveOrderService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private PartnerNotifier partnerNotifier;

    @Test
    void approvalTravelsFromOutboxThroughKafkaToPartnerNotification() {
        UUID partnerId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO partner (id, credit_limit, used_credit) VALUES (?, 100.00, 0)", partnerId);
        UUID orderId = createOrderService.create(partnerId,
                List.of(new OrderItem(UUID.randomUUID(), 1, new BigDecimal("10.00")))).id();

        approveOrderService.approve(orderId);

        verify(partnerNotifier, timeout(30_000))
                .notifyOrderStatusChanged(eq(orderId), argThat(payload -> payload.replace(" ", "").contains("\"newStatus\":\"APROVADO\"")));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM outbox_event WHERE aggregate_id = ? AND published_at IS NULL", Integer.class, orderId)));
    }

    @Test
    void duplicateDeliveryNotifiesPartnerOnlyOnce() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID duplicatedEventId = UUID.randomUUID();

        send(orderId, duplicatedEventId, "duplicated");
        send(orderId, duplicatedEventId, "duplicated");
        send(orderId, UUID.randomUUID(), "marker");

        verify(partnerNotifier, timeout(30_000)).notifyOrderStatusChanged(orderId, "marker");
        verify(partnerNotifier, times(1)).notifyOrderStatusChanged(orderId, "duplicated");
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT count(*) FROM processed_event WHERE event_id = ?", Integer.class, duplicatedEventId));
    }

    private void send(UUID orderId, UUID eventId, String payload) throws Exception {
        ProducerRecord<String, String> record = new ProducerRecord<>("order-status-changed", orderId.toString(), payload);
        record.headers().add(KafkaNotificationPublisher.EVENT_ID_HEADER, eventId.toString().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).get();
    }
}
