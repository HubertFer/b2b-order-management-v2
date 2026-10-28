package com.humberto.b2b.ordermanagement.order.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record OrderItem(UUID productId, int quantity, BigDecimal unitPrice) {

    public OrderItem {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be greater than zero");
        }
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must not be negative");
        }
    }
}
