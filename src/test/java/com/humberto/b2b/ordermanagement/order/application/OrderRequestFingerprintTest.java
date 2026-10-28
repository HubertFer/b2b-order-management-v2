package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderRequestFingerprintTest {

    private final UUID partnerId = UUID.randomUUID();
    private final UUID productA = UUID.randomUUID();
    private final UUID productB = UUID.randomUUID();

    @Test
    void producesSha256HexDigest() {
        String fingerprint = OrderRequestFingerprint.of(partnerId, List.of(item(productA, 1, "10.00")));

        assertTrue(fingerprint.matches("[0-9a-f]{64}"));
    }

    @Test
    void isStableForSameRequest() {
        assertEquals(OrderRequestFingerprint.of(partnerId, List.of(item(productA, 1, "10.00"))),
                OrderRequestFingerprint.of(partnerId, List.of(item(productA, 1, "10.00"))));
    }

    @Test
    void ignoresItemOrder() {
        assertEquals(
                OrderRequestFingerprint.of(partnerId, List.of(item(productA, 1, "10.00"), item(productB, 2, "5.00"))),
                OrderRequestFingerprint.of(partnerId, List.of(item(productB, 2, "5.00"), item(productA, 1, "10.00"))));
    }

    @Test
    void ignoresPriceScale() {
        assertEquals(OrderRequestFingerprint.of(partnerId, List.of(item(productA, 1, "10"))),
                OrderRequestFingerprint.of(partnerId, List.of(item(productA, 1, "10.00"))));
    }

    @Test
    void changesWithAnyRelevantField() {
        String original = OrderRequestFingerprint.of(partnerId, List.of(item(productA, 1, "10.00")));

        assertNotEquals(original, OrderRequestFingerprint.of(UUID.randomUUID(), List.of(item(productA, 1, "10.00"))));
        assertNotEquals(original, OrderRequestFingerprint.of(partnerId, List.of(item(productB, 1, "10.00"))));
        assertNotEquals(original, OrderRequestFingerprint.of(partnerId, List.of(item(productA, 2, "10.00"))));
        assertNotEquals(original, OrderRequestFingerprint.of(partnerId, List.of(item(productA, 1, "10.01"))));
        assertNotEquals(original, OrderRequestFingerprint.of(partnerId,
                List.of(item(productA, 1, "10.00"), item(productA, 1, "10.00"))));
    }

    private static OrderItem item(UUID productId, int quantity, String unitPrice) {
        return new OrderItem(productId, quantity, new BigDecimal(unitPrice));
    }
}
