package com.humberto.b2b.ordermanagement.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderItemTest {

    @Test
    void rejectsInvalidItemValues() {
        UUID productId = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> new OrderItem(null, 1, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> new OrderItem(productId, 1, null));
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(productId, 0, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(productId, 1, BigDecimal.ONE.negate()));
    }
}
