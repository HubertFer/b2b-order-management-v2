package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.domain.Order;

import java.util.List;

public record OrderPage(List<Order> orders, int page, int size, long totalElements, int totalPages) {

    public OrderPage {
        orders = List.copyOf(orders);
    }
}
