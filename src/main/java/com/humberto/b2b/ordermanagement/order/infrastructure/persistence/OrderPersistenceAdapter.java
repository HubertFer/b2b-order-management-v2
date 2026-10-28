package com.humberto.b2b.ordermanagement.order.infrastructure.persistence;

import com.humberto.b2b.ordermanagement.order.application.OrderPage;
import com.humberto.b2b.ordermanagement.order.application.OrderSearchCriteria;
import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderForUpdatePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SaveOrderPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SearchOrdersPort;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class OrderPersistenceAdapter implements SaveOrderPort, FindOrderForUpdatePort, FindOrderPort, SearchOrdersPort {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

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

    @Override
    @Transactional(readOnly = true)
    public OrderPage search(OrderSearchCriteria criteria, int page, int size) {
        Page<OrderEntity> result = repository.findAll(matching(criteria), PageRequest.of(page, size, NEWEST_FIRST));
        return new OrderPage(result.map(OrderEntity::toDomain).getContent(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }

    private static Specification<OrderEntity> matching(OrderSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.createdFrom() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), criteria.createdFrom()));
            }
            if (criteria.createdTo() != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), criteria.createdTo()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
