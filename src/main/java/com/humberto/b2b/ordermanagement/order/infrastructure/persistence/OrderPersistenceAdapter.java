package com.humberto.b2b.ordermanagement.order.infrastructure.persistence;

import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderForUpdatePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SaveOrderPort;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
class OrderPersistenceAdapter implements SaveOrderPort, FindOrderForUpdatePort, FindOrderPort {

    private final OrderJpaRepository repository;

    OrderPersistenceAdapter(OrderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Order save(Order order) {
        OrderEntity entity = repository.findById(order.id())
                .map(existing -> {
                    existing.updateStatus(order);
                    return existing;
                })
                .orElseGet(() -> repository.save(OrderEntity.fromDomain(order)));
        return entity.toDomain();
    }

    @Override
    @Transactional
    public Optional<Order> findByIdForUpdate(UUID orderId) {
        return repository.findByIdForUpdate(orderId).map(OrderEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID orderId) {
        return repository.findById(orderId).map(OrderEntity::toDomain);
    }
}
