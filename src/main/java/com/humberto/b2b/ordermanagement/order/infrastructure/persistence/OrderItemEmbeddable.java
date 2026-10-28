package com.humberto.b2b.ordermanagement.order.infrastructure.persistence;

import com.humberto.b2b.ordermanagement.order.domain.OrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class OrderItemEmbeddable {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    protected OrderItemEmbeddable() {
    }

    private OrderItemEmbeddable(UUID productId, int quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    static OrderItemEmbeddable fromDomain(OrderItem item) {
        return new OrderItemEmbeddable(item.productId(), item.quantity(), item.unitPrice());
    }

    OrderItem toDomain() {
        return new OrderItem(productId, quantity, unitPrice);
    }
}
