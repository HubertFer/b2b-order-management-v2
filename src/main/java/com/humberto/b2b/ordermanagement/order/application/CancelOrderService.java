package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderForUpdatePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.RecordOrderStatusChangePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.RefundPartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SaveOrderPort;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatusChangedEvent;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class CancelOrderService {

    private final FindOrderForUpdatePort findOrderForUpdatePort;
    private final RefundPartnerCreditPort refundPartnerCreditPort;
    private final SaveOrderPort saveOrderPort;
    private final RecordOrderStatusChangePort recordOrderStatusChangePort;
    private final Clock clock;

    public CancelOrderService(FindOrderForUpdatePort findOrderForUpdatePort,
                              RefundPartnerCreditPort refundPartnerCreditPort,
                              SaveOrderPort saveOrderPort,
                              RecordOrderStatusChangePort recordOrderStatusChangePort,
                              Clock clock) {
        this.findOrderForUpdatePort = findOrderForUpdatePort;
        this.refundPartnerCreditPort = refundPartnerCreditPort;
        this.saveOrderPort = saveOrderPort;
        this.recordOrderStatusChangePort = recordOrderStatusChangePort;
        this.clock = clock;
    }

    @Transactional
    public Order cancel(UUID orderId) {
        Order order = findOrderForUpdatePort.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Order cancelledOrder = order.transitionTo(OrderStatus.CANCELADO, clock.instant());
        if (order.hasDebitedCredit()) {
            refundPartnerCreditPort.refund(order.partnerId(), order.total());
        }
        Order savedOrder = saveOrderPort.save(cancelledOrder);
        recordOrderStatusChangePort.record(OrderStatusChangedEvent.between(order, savedOrder));
        return savedOrder;
    }
}
