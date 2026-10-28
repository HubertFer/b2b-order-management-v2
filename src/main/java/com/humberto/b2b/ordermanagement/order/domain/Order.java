package com.humberto.b2b.ordermanagement.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Order {

    private final UUID id;
    private final UUID partnerId;
    private final List<OrderItem> items;
    private final BigDecimal total;
    private final OrderStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Order(UUID id, UUID partnerId, List<OrderItem> items, BigDecimal total,
                  OrderStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.items = items;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order create(UUID partnerId, List<OrderItem> items, Instant createdAt) {
        Objects.requireNonNull(partnerId, "partnerId must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(items, "items must not be null");
        if (items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }

        List<OrderItem> copiedItems = List.copyOf(items);
        BigDecimal total = copiedItems.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Order(UUID.randomUUID(), partnerId, copiedItems, total,
                OrderStatus.PENDENTE, createdAt, createdAt);
    }

    public Order transitionTo(OrderStatus newStatus, Instant changedAt) {
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        Objects.requireNonNull(changedAt, "changedAt must not be null");
        if (!isTransitionAllowed(newStatus)) {
            throw new InvalidOrderTransitionException(status, newStatus);
        }
        return new Order(id, partnerId, items, total, newStatus, createdAt, changedAt);
    }

    private boolean isTransitionAllowed(OrderStatus newStatus) {
        return switch (status) {
            case PENDENTE -> newStatus == OrderStatus.APROVADO || newStatus == OrderStatus.CANCELADO;
            case APROVADO -> newStatus == OrderStatus.EM_PROCESSAMENTO || newStatus == OrderStatus.CANCELADO;
            case EM_PROCESSAMENTO -> newStatus == OrderStatus.ENVIADO || newStatus == OrderStatus.CANCELADO;
            case ENVIADO -> newStatus == OrderStatus.ENTREGUE;
            case ENTREGUE, CANCELADO -> false;
        };
    }

    public UUID id() {
        return id;
    }

    public UUID partnerId() {
        return partnerId;
    }

    public List<OrderItem> items() {
        return items;
    }

    public BigDecimal total() {
        return total;
    }

    public OrderStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}