package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;

import java.time.Instant;

public record OrderSearchCriteria(OrderStatus status, Instant createdFrom, Instant createdTo) {

    public OrderSearchCriteria {
        if (createdFrom != null && createdTo != null && !createdFrom.isBefore(createdTo)) {
            throw new IllegalArgumentException("createdFrom must be before createdTo");
        }
    }
}
