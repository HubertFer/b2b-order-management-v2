package com.humberto.b2b.ordermanagement.order.application;

import java.util.Objects;
import java.util.UUID;

public record IdempotencyRecord(UUID partnerId, String idempotencyKey, String requestHash, UUID orderId) {

    public IdempotencyRecord {
        Objects.requireNonNull(partnerId, "partnerId must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(requestHash, "requestHash must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
    }
}
