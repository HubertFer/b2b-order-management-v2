package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderForUpdatePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.RecordOrderStatusChangePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SaveOrderPort;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatusChangedEvent;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public class ChangeOrderStatusService {

    private final FindOrderForUpdatePort findOrderForUpdatePort;
    private final SaveOrderPort saveOrderPort;
    private final RecordOrderStatusChangePort recordOrderStatusChangePort;
    private final Clock clock;

    public ChangeOrderStatusService(FindOrderForUpdatePort findOrderForUpdatePort,
                                    SaveOrderPort saveOrderPort,
                                    RecordOrderStatusChangePort recordOrderStatusChangePort,
                                    Clock clock) {
        this.findOrderForUpdatePort = findOrderForUpdatePort;
        this.saveOrderPort = saveOrderPort;
        this.recordOrderStatusChangePort = recordOrderStatusChangePort;
        this.clock = clock;
    }

    @Transactional
    public Order changeStatus(UUID orderId, OrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        if (newStatus == OrderStatus.APROVADO || newStatus == OrderStatus.CANCELADO) {
            throw new IllegalArgumentException(
                    "Status " + newStatus + " must be set through its own operation because it affects partner credit");
        }
        Order order = findOrderForUpdatePort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Order savedOrder = saveOrderPort.save(order.transitionTo(newStatus, clock.instant()));
        recordOrderStatusChangePort.record(OrderStatusChangedEvent.between(order, savedOrder));
        return savedOrder;
    }
}
