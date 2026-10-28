package com.humberto.b2b.ordermanagement.order.api.dto;

import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Product, quantity and unit price of an ordered item")
public record OrderItemRequest(
        @Schema(description = "Product identifier", example = "33333333-3333-3333-3333-333333333333")
        @NotNull UUID productId,

        @Schema(description = "Units ordered, from 1 to 1,000,000", example = "2")
        @NotNull @Positive @Max(1_000_000) Integer quantity,

        @Schema(description = "Price of one unit, with at most two decimal places", example = "100.00")
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal unitPrice
) {

    public OrderItem toDomain() {
        return new OrderItem(productId, quantity, unitPrice);
    }
}
