package com.humberto.b2b.ordermanagement.order.application.port.out;

import com.humberto.b2b.ordermanagement.order.domain.Order;

import java.util.Optional;
import java.util.UUID;

public interface FindOrderForUpdatePort {
    Optional<Order> findByIdForUpdate(UUID orderId);
}
