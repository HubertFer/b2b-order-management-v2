package com.humberto.b2b.ordermanagement.order.application.port.out;

import com.humberto.b2b.ordermanagement.order.application.IdempotencyRecord;

import java.util.Optional;
import java.util.UUID;

public interface OrderIdempotencyPort {

    Optional<IdempotencyRecord> find(UUID partnerId, String idempotencyKey);

    boolean claim(IdempotencyRecord record);
}
