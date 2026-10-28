package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.domain.Order;

import java.util.Objects;

public record OrderCreation(Order order, boolean replayed) {

    public OrderCreation {
        Objects.requireNonNull(order, "order must not be null");
    }

    static OrderCreation created(Order order) {
        return new OrderCreation(order, false);
    }

    static OrderCreation replayed(Order order) {
        return new OrderCreation(order, true);
    }
}
