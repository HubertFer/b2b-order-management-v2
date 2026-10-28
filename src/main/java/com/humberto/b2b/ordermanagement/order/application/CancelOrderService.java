package com.humberto.b2b.ordermanagement.order.application;

import com.humberto.b2b.ordermanagement.order.application.port.out.FindOrderForUpdatePort;
import com.humberto.b2b.ordermanagement.order.application.port.out.RefundPartnerCreditPort;
import com.humberto.b2b.ordermanagement.order.application.port.out.SaveOrderPort;
import com.humberto.b2b.ordermanagement.order.domain.Order;
import com.humberto.b2b.ordermanagement.order.domain.OrderStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

public class CancelOrderService {

    private final FindOrderForUpdatePort findOrderForUpdatePort;
    private final RefundPartnerCreditPort refundPartnerCreditPort;
    private final SaveOrderPort saveOrderPort;
    private final Clock clock;

    public CancelOrderService(FindOrderForUpdatePort findOrderForUpdatePort,
                              RefundPartnerCreditPort refundPartnerCreditPort,
                              SaveOrderPort saveOrderPort,
                              Clock clock) {
        this.findOrderForUpdatePort = findOrderForUpdatePort;
        this.refundPartnerCreditPort = refundPartnerCreditPort;
        this.saveOrderPort = saveOrderPort;
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
        return saveOrderPort.save(cancelledOrder);
    }
}
