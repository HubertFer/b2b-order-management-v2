package com.humberto.b2b.ordermanagement.order.api.dto;

import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import com.humberto.b2b.ordermanagement.shared.api.MoneyFormat;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(UUID productId, int quantity, BigDecimal unitPrice) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(item.productId(), item.quantity(), MoneyFormat.of(item.unitPrice()));
    }
}
