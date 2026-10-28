package com.humberto.b2b.ordermanagement.order.api;

import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

record CreateOrderRequest(
        @NotNull UUID partnerId,
        @NotNull @Size(min = 1, max = 100) List<@NotNull @Valid OrderItemRequest> items
) {

    List<OrderItem> toDomainItems() {
        return items.stream().map(OrderItemRequest::toDomain).toList();
    }
}
