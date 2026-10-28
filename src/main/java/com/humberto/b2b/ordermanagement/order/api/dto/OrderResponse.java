package com.humberto.b2b.ordermanagement.order.api.dto;

import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import com.humberto.b2b.ordermanagement.shared.api.MoneyFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID partnerId,
        List<OrderItemResponse> items,
        BigDecimal total,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.id(),
                order.partnerId(),
                order.items().stream().map(OrderItemResponse::from).toList(),
                MoneyFormat.of(order.total()),
                order.status(),
                order.createdAt(),
                order.updatedAt()
        );
    }
}
