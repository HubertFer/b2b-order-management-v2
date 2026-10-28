package com.humberto.b2b.ordermanagement.order.infrastructure.persistence;

import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "order_item", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItemEmbeddable> items = new ArrayList<>();

    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean isNew;

    protected OrderEntity() {
    }

    static OrderEntity fromDomain(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.id = order.id();
        entity.partnerId = order.partnerId();
        entity.items = new ArrayList<>(order.items().stream().map(OrderItemEmbeddable::fromDomain).toList());
        entity.total = order.total();
        entity.status = order.status();
        entity.createdAt = order.createdAt();
        entity.updatedAt = order.updatedAt();
        entity.isNew = true;
        return entity;
    }

    void updateStatus(Order order) {
        this.status = order.status();
        this.updatedAt = order.updatedAt();
    }

    Order toDomain() {
        return Order.restore(id, partnerId, items.stream().map(OrderItemEmbeddable::toDomain).toList(),
                total, status, createdAt, updatedAt);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}