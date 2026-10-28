package com.humberto.b2b.ordermanagement.order.api.dto;

import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemRequest(
        @NotNull UUID productId,
        @NotNull @Positive @Max(1_000_000) Integer quantity,
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal unitPrice
) {

    public OrderItem toDomain() {
        return new OrderItem(productId, quantity, unitPrice);
    }
}
